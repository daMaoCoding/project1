package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.repository.FinBalanceStatRepository;
import com.xinbo.fundstransfer.service.FinBalanceStatService;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinBalanceStatServiceImpl implements FinBalanceStatService {
	@Autowired
	private FinBalanceStatRepository finBalanceStatRepository;
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FinBalanceStatServiceImpl.class);

	/**
	 * 余额明细
	 */
	@Override
	public Map<String, Object> finBalanceStat(PageRequest pageRequest) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finBalanceStatRepository.queyfinBalanceStat(pageRequest);
		map.put("Page", dataToPage);
		return map;
	}

	/**
	 * 余额明细>明细
	 */
	@Override
	public Map<String, Object> finBalanceStatCard(int id, String account, int status, String bankType,
			PageRequest pageRequest) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		logger.debug("调用余额明细>明细 参数 id：{},account：{},pageRequest：{}：", id, account, pageRequest);
		Page<Object> dataToPage = null;
		java.lang.Object[] total = null;
		// 根据不同的id查询不同类型的数据
		List<Integer> type = new ArrayList<>();
		List<Integer> statuss = new ArrayList<>();
		if (status == 1) {
			statuss.add(1);
			statuss.add(5);
		} else {
			statuss.add(status);
		}
		if (1 == id) {
			// 入款账号的明细
			dataToPage = finBalanceStatRepository.queyfinBalanceStatCardIn(account, statuss, bankType, pageRequest);
			// 查询总计进行返回
			total = finBalanceStatRepository.totalfinBalanceStatCardIn(account, statuss, bankType);
		} else if (2 == id) {
			// 出款账号的明细
			dataToPage = finBalanceStatRepository.queyfinBalanceStatCardOut(account, statuss, bankType, pageRequest);
			// 查询总计进行返回
			total = finBalanceStatRepository.totalfinBalanceStatCardOut(account, statuss, bankType);
		} else if (3 == id) {
			// 备用金卡的明细
			type.add(13);
			dataToPage = finBalanceStatRepository.queyfinBalanceStatCardReserveBank(type, account, statuss, bankType,
					pageRequest);
			// 查询总计进行返回
			total = finBalanceStatRepository.totalfinBalanceStatCardReserveBank(type, account, statuss, bankType);
		} else if (4 == id) {
			// 现金卡的明细
			type.add(8);
			dataToPage = finBalanceStatRepository.queyfinBalanceStatCardReserveBank(type, account, statuss, bankType,
					pageRequest);
			// 查询总计进行返回
			total = finBalanceStatRepository.totalfinBalanceStatCardReserveBank(type, account, statuss, bankType);
		} else if (5 == id) {
			// 现金卡的明细
			type.add(9);
			dataToPage = finBalanceStatRepository.queyfinBalanceStatCardReserveBank(type, account, statuss, bankType,
					pageRequest);
			// 查询总计进行返回
			total = finBalanceStatRepository.totalfinBalanceStatCardReserveBank(type, account, statuss, bankType);
		} else if (6 == id) {
			// 第三方入款余额明细
			type.add(1);
			type.add(3);
			type.add(4);
			dataToPage = finBalanceStatRepository.queyfinBalanceStatCardReserveBank(type, account, statuss, bankType,
					pageRequest);
			// 查询总计进行返回
			total = finBalanceStatRepository.totalfinBalanceStatCardReserveBank(type, account, statuss, bankType);
		} else if (7 == id) {
			dataToPage = finBalanceStatRepository.queyfinBalanceStatCardNotissued(account, statuss, bankType,
					pageRequest);
			// 查询总计进行返回
			total = finBalanceStatRepository.totalfinBalanceStatCardNotissued(account, statuss, bankType);
		} else if (8 == id) {
			type.add(5);
			dataToPage = finBalanceStatRepository.queyfinBalanceStatCardReserveBank(type, account, statuss, bankType,
					pageRequest);
			// 查询总计进行返回
			total = finBalanceStatRepository.totalfinBalanceStatCardReserveBank(type, account, statuss, bankType);
		}
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	/**
	 * 余额明细>明细>系统明细
	 */
	@Override
	public Map<String, Object> finTransBalanceSys(String to_account, String from_account, String fristTime,
			String lastTime, BigDecimal startamount, BigDecimal endamount, int accountid, int id, String type,
			String accounttype, String accountname, PageRequest pageRequest) throws Exception {
		logger.debug(
				"调用余额明细>明细>系统明细 参数 to_account：{},from_account：{},fristTime：{},lastTime：{},startamount：{},endamount:{},accountid:{},id:{},type:{},accounttype:{},pageRequest：{}：",
				to_account, from_account, fristTime, lastTime, startamount, endamount, accountid, id, type, accounttype,
				pageRequest);
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = null;
		java.lang.Object[] total = null;
		if ("sys".equals(type)) {
			// 根据不同的id查询不同类型的数据 (系统的流水)
			if (1 == id) {
				// 处理入款账号里面的第三方的数据
				if ("2".equals(accounttype)) {
					// 第三方入款余额数据源
					dataToPage = finBalanceStatRepository.queyfinTransBalanceThirdSys(to_account, from_account,
							fristTime, lastTime, startamount, endamount, accountname, pageRequest);
					// 查询总计进行返回
					total = finBalanceStatRepository.totalfinTransBalanceThirdSys(to_account, from_account, fristTime,
							lastTime, startamount, endamount, accountname);
				} else {
					// 入款账号数据源
					dataToPage = finBalanceStatRepository.queyfinTransBalanceInSys(to_account, from_account, fristTime,
							lastTime, startamount, endamount, accountid, pageRequest);
					// 查询总计进行返回
					total = finBalanceStatRepository.totalfinTransBalanceInSys(to_account, from_account, fristTime,
							lastTime, startamount, endamount, accountid);
				}

			} else if (2 == id) {
				// 出款账号数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceOutSys(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceOutSys(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid);
			} else if (3 == id) {
				// 备用金卡数据源(有进有出)
				dataToPage = finBalanceStatRepository.queyfinTransBalanceReservePettycashSys(to_account, from_account,
						fristTime, lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceReservePettycashSys(to_account, from_account,
						fristTime, lastTime, startamount, endamount, accountid);
			} else if (4 == id) {
				// 现金卡数据源(只进不如，当成入款卡查询数据)
				dataToPage = finBalanceStatRepository.queyfinTransBalanceCashBankSys(to_account, from_account,
						fristTime, lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceCashBankSys(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid);

			} else if (5 == id) {
				// 公司入款余额数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceCompanySys(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceCompanySys(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid);
			} else if (6 == id) {
				// 第三方入款余额数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceThirdSys(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountname, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceThirdSys(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountname);
			} else if (7 == id) {
				// 未下发余额数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceNotissuedSys(to_account, from_account,
						fristTime, lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceNotissuedSys(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid);
			} else if (8 == id) {
				//////////////////////////////////// 暂时没做这个功能
				// 可用余额数据源
			}
		} else if ("bank".equals(type)) {
			// 根据不同的id查询不同类型的数据 (系统的流水)
			if (1 == id) {
				// 入款账号数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceInBank(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceInBank(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid);
			} else if (2 == id) {
				// 出款账号数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceOutBank(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceOutBank(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid);
			} else if (3 == id) {
				// 备用金卡数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceReservePettycashBank(to_account, from_account,
						fristTime, lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceReservePettycashBank(to_account, from_account,
						fristTime, lastTime, startamount, endamount, accountid);
			} else if (4 == id) {
				// 现金卡数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceCashBank(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceCashBank(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid);
			} else if (5 == id) {
				// 公司入款余额数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceCompanyBank(to_account, from_account,
						fristTime, lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceCompanyBank(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid);
			} else if (6 == id) {
				// 第三方入款余额数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceThirdBank(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceThirdBank(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid);
			} else if (7 == id) {
				// 未下发余额数据源
				dataToPage = finBalanceStatRepository.queyfinTransBalanceNotissuedBank(to_account, from_account,
						fristTime, lastTime, startamount, endamount, accountid, pageRequest);
				// 查询总计进行返回
				total = finBalanceStatRepository.totalfinTransBalanceNotissuedBank(to_account, from_account, fristTime,
						lastTime, startamount, endamount, accountid);
			} else if (8 == id) {
				// 可用余额数据源
			}
		}

		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	/**
	 * 查询没有匹配的数据
	 */
	@Override
	public Map<String, Object> ClearAccountDate(String fristTime, PageRequest pageRequest) throws Exception {
		logger.debug("调用 清算数据，查询是否还存在没有匹配的数据 参数 fristTime:{},pageRequest：{}：", fristTime, pageRequest);
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = null;
		dataToPage = finBalanceStatRepository.ClearAccountDate(fristTime, pageRequest);
		map.put("Page", dataToPage);
		return map;
	}

	/**
	 * 删除已经匹配的数据
	 */
	@Override
	@Transactional
	public Map<String, Object> DeleteAccountDate(String fristTime) throws Exception {
		logger.info("调用 删除已经匹配的数据  参数 fristTime:{}:", fristTime);
		Map<String, Object> map = new HashMap<String, Object>();
		// 删除之前检查是否还有需要删除的数据
		List<Object> list = finBalanceStatRepository.checkData(fristTime);
		if (list.size() > 0) {
			// 删除入款表的数据incom
			finBalanceStatRepository.DeleteAccounIncomtDate(fristTime);
			// 删除出款请求表的数据 outward_request
			finBalanceStatRepository.DeleteAccountOutwardRequestDate(fristTime);
			// 删除出款任务表的数据 outward_task
			finBalanceStatRepository.DeleteAccountOutwardTaskDate(fristTime);
			// 因为强制清算的时候
			finBalanceStatRepository.DeleteTransactionDate(fristTime);
			// 因为强制清算的时候
			finBalanceStatRepository.DeletebankLogDate(fristTime);
			// 删除第三方的数据
			finBalanceStatRepository.DeleteThirdRequestDate(fristTime);
			// Thread.currentThread().sleep(1000);// 毫秒
			// DeleteAccountDate(fristTime);
		}
		map.put("message", "清算成功！");

		return map;
	}

	/**
	 * 删除保存到服务器的出款成功截图
	 */
	// 删除文件夹
	public void delFolder(String path, String startTime) {
		try {
			startTime = startTime.substring(0, 10).replace("-", "");
			File file = new File(path);
			if (!file.exists()) {
				return;
			}
			if (!file.isDirectory()) {
				return;
			}
			String[] tempList = file.list();
			for (int i = 0; i < tempList.length; i++) {
				if (Integer.parseInt(tempList[i]) < Integer.parseInt(startTime)) {
					deletedScreenshots(path + "/" + tempList[i]); // 删除完里面所有内容
					String filePath = path + "/" + tempList[i];
					filePath = filePath.toString();
					java.io.File myFilePath = new java.io.File(filePath);
					myFilePath.delete(); // 删除空文件夹
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void deletedScreenshots(String path) throws Exception {
		File file = new File(path);
		if (!file.exists()) {
			return;
		}
		if (!file.isDirectory()) {
			return;
		}
		String[] tempList = file.list();
		File temp = null;
		for (int i = 0; i < tempList.length; i++) {
			if (path.endsWith(File.separator)) {
				temp = new File(path + tempList[i]);
			} else {
				temp = new File(path + File.separator + tempList[i]);
			}
			// 文件夹里面是否还有文件未删除
			if (temp.isFile()) {
				temp.delete();
			}
			if (temp.isDirectory()) {
				deletedScreenshots(path + "/" + tempList[i]);// 先删除文件夹里面的文件
				deletedScreenshots(path + "/" + tempList[i]);// 再删除空文件夹
			}
		}
		if (file.exists() && tempList.length <= 0) {
			file.delete();
		}
	}

	@Override
	public Map<String, Object> finbalanceEveryDay(int handicapId, String startTime, String endTime,
			PageRequest pageRequest) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finBalanceStatRepository.finbalanceEveryDay(handicapId, startTime, endTime,
				pageRequest);
		map.put("Page", dataToPage);
		return map;
	}

	@Override
	public Map<String, Object> findBalanceDetail(String account, String bankType, int handicap, String time, int type,
			int status, PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		List<Integer> types = new ArrayList<>();
		List<Integer> statuss = new ArrayList<>();
		if (status == 1) {
			statuss.add(1);
			statuss.add(5);
		} else {
			statuss.add(status);
		}
		if (type == 1) {
			types.add(1);
		} else if (type == 2) {
			types.add(5);
		} else if (type == 3) {
			types.add(13);
		} else if (type == 4) {
			types.add(8);
		} else if (type == 5) {
			types.add(9);
		} else if (type == 6) {
			types.add(1);
			types.add(2);
			types.add(3);
		} else if (type == 7) {
			types.add(2);
		}
		dataToPage = finBalanceStatRepository.findBalanceDetail(account, bankType, handicap, time, types, statuss,
				pageRequest);
		// 查询总计进行返回
		total = finBalanceStatRepository.totalFindBalanceDetail(account, bankType, handicap, time, types, statuss);
		map.put("rebatePage", dataToPage);
		map.put("rebateTotal", total);
		return map;
	}
}
