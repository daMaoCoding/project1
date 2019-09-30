package com.xinbo.fundstransfer.report.up;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.xinbo.fundstransfer.report.SystemAccountCommon;
import com.xinbo.fundstransfer.report.SystemAccountConfiguration;
import com.xinbo.fundstransfer.report.fail.FailHandler;
import com.xinbo.fundstransfer.report.outlet.OutletHandler;
import com.xinbo.fundstransfer.report.patch.PatchHandler;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.survey.InvstHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.service.AccountService;

@Configuration
public class ReportHandler extends ApplicationObjectSupport {
	private static final Logger log = LoggerFactory.getLogger(ReportHandler.class);
	private static final Map<String, Report> dealMap = new LinkedHashMap<>();
	private static final int BATCH_SIZE = 1000;
	@Autowired
	@Lazy
	private AccountService accSer;
	@Autowired
	private FailHandler failHandler;
	@Autowired
	private SuccessHandler successHandler;
	@Autowired
	private InvstHandler invstHandler;
	@Autowired
	private RedisTemplate accountingRedisTemplate;
	@Autowired
	private StringRedisTemplate accountingStringRedisTemplate;
	@Autowired
	private StoreHandler storeHandler;
	@Autowired
	private PatchHandler patchHandler;
	@Autowired
	private OutletHandler outletHandler;
	private ExecutorService executor = Executors.newFixedThreadPool(60);

	@PostConstruct
	public void init() {
		Map<String, Object> map = super.getApplicationContext().getBeansWithAnnotation(ReportUp.class);
		map.forEach((k, v) -> dealMap.put(k, (Report) v));
	}

	/**
	 * 上报处理 入口
	 *
	 */
	@SuppressWarnings("unchecked")
	public void deal() throws Exception {
		if (!SystemAccountConfiguration.mainOperatingSwitch() || !SystemAccountCommon.checkHostRunRight()
				|| !storeHandler.finishLoad()) {
			Thread.sleep(4000);
			return;
		}
		List<String> datas = accountingRedisTemplate.opsForList().range(RedisTopics.SYS_BAL_RPUSH, 0, BATCH_SIZE);
		if (datas.size() == 0) {
			Thread.sleep(1000);
			return;
		}
		accountingRedisTemplate.opsForList().trim(RedisTopics.SYS_BAL_RPUSH, datas.size(), -1);
		Map<String, List<String>> infoMap = datas.stream().collect(Collectors.groupingBy(s -> {
			if (null != s) {
				String[] ss = s.split(SysBalUtils.SEPARATOR);
				if (ss.length == 3) {
					return ss[1];
				} else {
					return "0";
				}
			} else {
				return "0";
			}
		}));
		CountDownLatch latch = new CountDownLatch(infoMap.size());
		for (Entry<String, List<String>> info : infoMap.entrySet()) {
			executor.submit(() -> subDeal(info.getKey(), accountingStringRedisTemplate, info.getValue(), latch));
		}
		latch.await();
		outletHandler.computeBalancing(accountingStringRedisTemplate);
		ReportCheck.clear(accountingStringRedisTemplate);
		invstHandler.autoClearError(accountingStringRedisTemplate);
	}

	private void subDeal(String targetId, StringRedisTemplate template, List<String> dataList, CountDownLatch latch) {
		try {
			AccountBaseInfo base = accSer.getFromCacheById(Integer.valueOf(targetId));
			if (Objects.isNull(base))
				return;
			if (dataList.size() == 0)
				return;
			ReportCheck reportCheck = new ReportCheck(base,
					(p) -> template.keys(SysBalTrans.genPatternFrId(base.getId())).stream().map(SysBalTrans::new)
							.collect(Collectors.toList()),
					(p) -> template.keys(SysBalTrans.genPatternToId(base.getId())).stream().map(SysBalTrans::new)
							.collect(Collectors.toList()));
			for (String data : dataList) {
				String[] infs = data.split(SysBalUtils.SEPARATOR);
				int len = infs.length;
				if (len != 3)
					return;
				if (!StringUtils.isNumeric(targetId))
					return;
				String classType = Objects.equals(base.getType(), AccountType.InBank.getTypeId())
						? Report.ACC_TYPE_INBANK : Report.ACC_TYPE_OTHERS;
				try {
					dealMap.get(Report.REPORT_UP + classType + infs[0]).deal(template, infs[2], reportCheck);
				} catch (Exception e) {
					log.error("处理系统流水错误", e);
				}
			}
			successHandler.yunSF(template, base, reportCheck);
			failHandler.record(base, reportCheck);
			if (!patchHandler.init0(template, reportCheck))
				patchHandler.init1(template, reportCheck);
			patchHandler.yunSFAbsentFlow(template, reportCheck);
			patchHandler.yunSFInomeSameAmount(template, reportCheck);
			patchHandler.quarterInterest(template, reportCheck);
			patchHandler.sort0(template, reportCheck);
			patchHandler.depositSameAmount(template, reportCheck);
			outletHandler.pushReprotCheck(reportCheck);
			invstHandler.invst(template, reportCheck);
			if (Objects.equals(AccountType.OutBank.getTypeId(), base.getType()) && Objects.nonNull(base.getHolder())) {
				// 人工出款：用流水来确认，不在进行出一笔卡一笔
				template.boundHashOps(RedisKeys.SYS_BAL_IN).delete(targetId);
				template.boundHashOps(RedisKeys.SYS_BAL_OUT).delete(targetId);
			} else {
				// 机器出款
				String vIn = (String) template.boundHashOps(RedisKeys.SYS_BAL_IN).get(targetId);
				if (StringUtils.isNotBlank(vIn)) {
					SysBalTrans tsIn = new SysBalTrans(vIn);
					if (SysBalTrans.SYS_REFUND == tsIn.getSys() || tsIn.ackTo()
							|| tsIn.getGetTm() > 0 && System.currentTimeMillis() - tsIn.getGetTm() > 600000) {
						template.boundHashOps(RedisKeys.SYS_BAL_IN).delete(targetId);
					}
				}
				String vOut = (String) template.boundHashOps(RedisKeys.SYS_BAL_OUT).get(targetId);
				if (StringUtils.isNotBlank(vOut)) {
					SysBalTrans tsOut = new SysBalTrans(vOut);
					if (SysBalTrans.SYS_REFUND == tsOut.getSys() || tsOut.ackFr()
							|| tsOut.getGetTm() > 0 && System.currentTimeMillis() - tsOut.getGetTm() > 150000) {
						template.boundHashOps(RedisKeys.SYS_BAL_OUT).delete(targetId);
					}
				}
			}
		} finally {
			latch.countDown();
		}
	}
}
