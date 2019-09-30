package com.xinbo.fundstransfer.report.up;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountCommon;
import com.xinbo.fundstransfer.report.acc.ErrorHandler;
import com.xinbo.fundstransfer.report.acc.SysAccPush;
import com.xinbo.fundstransfer.report.fail.FailHandler;
import com.xinbo.fundstransfer.report.init.InitHandler;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.report.streamalarm.StreamAlarmHandler;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Lazy;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;

public abstract class Report {

	protected static final Logger log = LoggerFactory.getLogger(Report.class);

	/* 处理上报 注解前缀 */
	public static final String REPORT_UP = "REPORT_UP";
	/* 处理上报 入款卡 */
	public static final String ACC_TYPE_INBANK = "INBANK";
	/* 处理上报 其他卡 */
	public static final String ACC_TYPE_OTHERS = "OTHERS";

	public static final long EXPIRE_MILLIS_ENTITY = 60000 * 15;

	public static final long EXPIRE_MILLIS_OPPBAL = 60000 * 30;

	public static final long EXPIRE_MILLIS_REFUND = 60000 * 30;

	public static final long EXPIRE_MILLIS_CURBAL = 60000 * 15;

	public static final long VALID_MILLIS_CURBAL = 30000;// 30秒

	public static int FEE_TOLERANCE = 5;

	protected static final int ACK_FR = 1;
	protected static final int ACK_TO = 2;

	protected static int Refunding = BankLogStatus.Refunding.getStatus();
	protected static int Refunded = BankLogStatus.Refunded.getStatus();
	protected static int Interest = BankLogStatus.Interest.getStatus();
	protected static int Fee = BankLogStatus.Fee.getStatus();

	protected static int InBank = AccountType.InBank.getTypeId();
	protected static int BindWechat = AccountType.BindWechat.getTypeId();
	protected static int BindAli = AccountType.BindAli.getTypeId();
	protected static int ThirdCommon = AccountType.ThirdCommon.getTypeId();
	protected static int BindCommon = AccountType.BindCommon.getTypeId();
	protected static int OutBank = AccountType.OutBank.getTypeId();

	protected static int IN_BANK_YSF = InBankSubType.IN_BANK_YSF.getSubType();
	protected static int IN_BANK_YSF_MIX = InBankSubType.IN_BANK_YSF_MIX.getSubType();

	protected static int ACCOUNT_FLAG_REFUND = AccountFlag.REFUND.getTypeId();

	@Autowired
	@Lazy
	protected AccountService accSer;
	@Autowired
	protected ErrorHandler errHandler;
	@Autowired
	protected RedisService redisSer;
	@Autowired
	protected CabanaService cabanaSer;
	@Autowired
	protected SysUserService userSer;
	@Autowired
	protected FailHandler failHandler;
	@Autowired
	protected SuccessHandler successHandler;
	@Autowired
	protected InitHandler initHandler;
	@Autowired
	protected SystemAccountCommon systemAccountCommon;
	@Autowired
	protected StoreHandler storeHandler;
	@Autowired
	protected StreamAlarmHandler streamAlarmHandler;
	protected ObjectMapper mapper = new ObjectMapper();

	/**
	 * 每个场景的具体逻辑处理接口
	 */
	protected abstract void deal(StringRedisTemplate template, String rpushData, ReportCheck check) throws Exception;

	protected void reRegist(StringRedisTemplate template, TransferEntity entity, long TASK_ID, BigDecimal TR_AMT,
			int FR_ID, int TO_ID, String TO_ACC_3, String TO_OWN_2, ReportCheck check) {
		if (Objects.nonNull(entity) && Objects.nonNull(entity.getAcquireTime()) && entity.getAcquireTime() > 0
				&& System.currentTimeMillis() - entity.getAcquireTime() > 1000 * 418)
			return;
		// 检查REDIS中有无该记录
		List<SysBalTrans> tsList = check.getTransOutAll().stream()
				.filter(p -> TASK_ID > 0 && TASK_ID == p.getTaskId() || TR_AMT.compareTo(p.getAmt()) == 0
						&& (TO_ID > 0 && TO_ID == p.getToId() || Objects.equals(TO_ACC_3, p.getToAcc3Last())
								|| Objects.equals(TO_OWN_2, p.getToOwn2Last())))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(tsList))
			return;
		// 再次检查数据库中有无该记录
		// 判定标准：
		// 1.orderId is null, 是否有相同金额
		// 2.orderId is not null, 订单ID是否相等
		if (storeHandler.findSysLogFromCache(FR_ID).stream()
				.filter(p -> Objects.isNull(p.getOrderId()) && Objects.nonNull(p.getAmount())
						&& p.getAmount().abs().compareTo(TR_AMT) == 0
						|| Objects.nonNull(p.getOrderId()) && TASK_ID == p.getOrderId())
				.count() > 0)
			return;
		if (CollectionUtils.isEmpty(tsList)) {
			String k = systemAccountCommon.reRegist(template, entity);
			check.reRegist(new SysBalTrans(k));
			log.info("SB{} SB{} [ TRANSFER ENTITY RE-REGIST ] >>  taskId: {} amt: {}  msg: {}", FR_ID, TO_ID, TASK_ID,
					TR_AMT, k);
		}
	}

	/**
	 * 获取银行余额基准
	 */
	protected BigDecimal benchmark(StringRedisTemplate template, Integer target) {
		if (Objects.isNull(template) || Objects.isNull(target))
			return BigDecimal.ZERO;
		String val = (String) template.boundHashOps(RedisKeys.REAL_BAL_BENCHMARK).get(target.toString());
		return StringUtils.isEmpty(val) ? BigDecimal.ZERO : SysBalUtils.radix2(new BigDecimal(val));
	}

	/**
	 * 设置银行余额基准
	 */
	protected void benchmark(StringRedisTemplate template, Integer target, BigDecimal val) {
		if (Objects.isNull(template) || Objects.isNull(target) || Objects.isNull(val)
				|| val.compareTo(BigDecimal.ZERO) <= 0)
			return;
		BigDecimal his = benchmark(template, target);
		if (his.compareTo(val) != 0)
			template.boundHashOps(RedisKeys.REAL_BAL_BENCHMARK).put(target.toString(),
					SysBalUtils.radix2(val).toString());
		// accSer.updateBankBalance(val, target);
	}

	protected void feeFist(StringRedisTemplate template, BizBankLog lg) {
		BizSysLog flg = storeHandler.find4Fee(lg.getFromAccount(), CommonUtils.getStartTimeOfCurrDay());
		if (Objects.isNull(flg) || Objects.isNull(flg.getFee()) || flg.getFee().compareTo(BigDecimal.ZERO) != 0)
			return;
		flg.setFee(lg.getAmount());
		storeHandler.setSysBal(template, lg.getFromAccount(), lg.getAmount().abs(), null, true);
		if (Objects.nonNull(flg.getBalance()) && flg.getBalance().compareTo(BigDecimal.ZERO) != 0)
			flg.setBankBalance(lg.getBalance());
		if (Objects.nonNull(flg.getBalance()))
			flg.setBalance(flg.getBalance().subtract(lg.getAmount().abs()));
		storeHandler.saveAndFlush(flg);
	}

	/**
	 * 目前只是做清理工作：处理为失败暂未处理
	 */
	protected void post4BankFlow(StringRedisTemplate template, AccountBaseInfo base, List<SysBalTrans> outList) {
		if (CollectionUtils.isEmpty(outList))
			return;
		List<SysBalTrans> l = outList.stream().filter(p -> p.ackFrByFlow())
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(l))
			return;
		long tradeTm = l.get(0).getGetTm();
		List<SysBalTrans> unkownList = outList.stream().filter(p -> tradeTm > p.getGetTm()
				&& SysBalTrans.SYS_REFUND != p.getSys() && !p.ackFr() && (p.getToId() == 0 || !p.ackTo()))
				.collect(Collectors.toList());
		String kOut = (String) template.boundHashOps(RedisKeys.SYS_BAL_OUT).get(String.valueOf(base.getId()));
		SysBalTrans tsOut = StringUtils.isEmpty(kOut) ? null : new SysBalTrans(kOut);
		if (Objects.nonNull(tsOut) && tsOut.getGetTm() != 0 && tradeTm >= tsOut.getGetTm()
				&& SysBalTrans.SYS_REFUND != tsOut.getSys() && !tsOut.ackFr()
				&& (tsOut.getToId() == 0 || !tsOut.ackTo())) {
			template.boundHashOps(RedisKeys.SYS_BAL_OUT).delete(String.valueOf(tsOut.getFrId()));
		}
		for (SysBalTrans tmp : unkownList) {
			template.delete(tmp.getMsg());
			if (tmp.getToId() != 0) {// IN 清理
				String kIn = (String) template.boundHashOps(RedisKeys.SYS_BAL_IN).get(String.valueOf(tmp.getToId()));
				if (StringUtils.isNotEmpty(kIn) && SysBalUtils.equal(new SysBalTrans(kIn), tmp)) {
					template.boundHashOps(RedisKeys.SYS_BAL_IN).delete(String.valueOf(tmp.getToId()));
				}
			}
		}
		// 记录无效出款记录
		String remark = SysBalUtils.autoRemark("转账时间无效");
		for (SysBalTrans tmp : unkownList) {
			tmp.setInvalidTransferTime(true);
			errHandler.handle(new SysAccPush(tmp.getFrId(), SysAccPush.ActionInvalidTransfer, tmp, remark),null);
		}
	}

	protected void outward2Mgr4Man(StringRedisTemplate template, BizOutwardTask task, ReportCheck check) {
		List<SysBalTrans> tsList = check.getTransOutAll().stream()
				.filter(p -> Objects.equals(task.getId(), p.getTaskId())
						&& SysBalTrans.TASK_TYPE_OUTMEMEBER == p.getTaskType() && SysBalTrans.SYS_REFUND != p.getSys()
						&& !p.ackFr())
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(tsList))
			return;
		SysBalTrans ts = tsList.get(0);
		ts.setAckTm(System.currentTimeMillis());
		ts.setRegistWay(SysBalTrans.REGIST_WAY_MAN_MGR);
		successHandler.reWriteMsg(template, ts, 90, TimeUnit.MINUTES);
		log.info("SB{} [  OUTWARD TO MANGER MANUAL ] >> taskId: {} orderNo: {} toAccount: {} toOwner: {} amount: {}",
				task.getAccountId(), task.getId(), task.getOrderNo(), task.getToAccount(), task.getToAccountOwner(),
				task.getAmount());
	}

	protected void lasttime(StringRedisTemplate template, Integer id) {
		if (Objects.nonNull(template) && Objects.nonNull(id)) {
			template.boundHashOps(RedisKeys.REAL_BAL_LASTTIME).put(String.valueOf(id),
					String.valueOf(System.currentTimeMillis()));
		}
	}

	/**
	 * 向工具端发送抓取流水指令
	 */
	protected void sendCapture(StringRedisTemplate template, AccountBaseInfo base, BigDecimal sysBal,
			BigDecimal bankBal) {
		if (Objects.isNull(sysBal) || Objects.isNull(bankBal) || Objects.equals(InBank, base.getType()))
			return;
		if (bankBal.compareTo(sysBal) > 0)
			return;
		long lastLgTm = systemAccountCommon.crawlTime4BankStatement(template, base.getId());
		// 如果在2分钟内，已经抓取过流水，不发送该指令
		if (Objects.nonNull(lastLgTm) && System.currentTimeMillis() - lastLgTm < 240000) {
			log.info("INVST{} [ CAPTURE SENDED ] >> bal: {} bankBal: {},sec: {} ", base.getId(), sysBal, bankBal,
					(System.currentTimeMillis() - lastLgTm) / 1000);
			return;
		}
		boolean send = true;
		// 向PC,工具发送抓取流水指令
		try {
			if (Objects.isNull(base.getFlag()) || Objects.equals(base.getFlag(), AccountFlag.PC.getTypeId())) {// PC
				MessageEntity<Integer> msg = new MessageEntity<>();
				msg.setAction(ActionEventEnum.CAPTURE.ordinal());
				msg.setData(base.getId());
				msg.setIp(String.valueOf(base.getId()));
				redisSer.convertAndSend(RedisTopics.PUSH_MESSAGE_TOOLS, mapper.writeValueAsString(msg));
			} else {// 手机|兼职返利
				ResponseData<?> r = cabanaSer.conciliate(base.getId(),
						new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
				send = Objects.nonNull(r) && r.getStatus() == GeneralResponseData.ResponseStatus.SUCCESS.getValue();
			}
		} catch (Exception e) {
			send = false;
		}
		if (send) {
			systemAccountCommon.crawlTime4BankStatement(template, base.getId(), System.currentTimeMillis());
			log.info("INVST{} [ CAPTURE SUCCESS ] >> send capture redirect to APP|client. bal: {} bankBal: {} ",
					base.getId(), sysBal, bankBal);
		} else {
			log.info("INVST{} [ CAPTURE FAIL ] >> send capture redirect to APP|client. bal: {} bankBal: {} ",
					base.getId(), sysBal, bankBal);
		}
	}
}