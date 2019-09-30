package com.xinbo.fundstransfer.runtime;

import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.runtime.task.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 多线程并发工具类
 */
public class ConcurrentUtils {
	private static final Logger logger = LoggerFactory.getLogger(ConcurrentUtils.class);
	private static volatile ConcurrentUtils instance;
	private ExecutorService executorService;
	private ScheduledExecutorService scheduledExecutorService;
	private IncomeHandler incomeHandler;
	private OutwardHandler outwardHandler;
	// private BankLogHandler bankLogHandler;
	private ThirdLogHandler thirdLogHandler;
	private ThirdRequestHandler thirdRequestHandler;
	private OutwardTaskAllocateHandler outwardTaskAllocateHandler;
	private TransferAllocateHandler transferAllocateHandler;
	private SysBalHandler sysBalHandler;
	private AccInvHandler accInvHandler;
	private OutwardTaskTurnHandler outwardTaskTurnHandler;
	// private WechatLogHandler wechatLogHandler;
	// private AliPayLogHandler aliPayLogHandler;
	/**
	 * 定时取消超时的入款请求
	 */
	private ScheduledFuture<?> cancelIncomeRequest;

	/**
	 * 同步新平台出入款订单
	 */
	private ScheduledFuture<?> pullOrdersSchedule;
	private ScheduledFuture<?> pullIncomeOrdersSchedule;
	private ScheduledFuture<?> handicapNotifyUrlSchedule;
	private ScheduledFuture<?> handicapDaifuSyncSchedule;
	private ScheduledFuture<?> daifuInfoPayCoreResultSyncSchedule;

	private ConcurrentUtils() {
		executorService = Executors.newCachedThreadPool();
		scheduledExecutorService = Executors.newScheduledThreadPool(16);
	}

	public static ConcurrentUtils getInstance() {
		if (instance == null) {
			synchronized (ConcurrentUtils.class) {
				if (instance == null) {
					instance = new ConcurrentUtils();
				}
			}
		}
		return instance;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public ScheduledExecutorService getScheduledExecutorService() {
		return scheduledExecutorService;
	}

	public void shutdown() {
		executorService.shutdown();
		scheduledExecutorService.shutdown();
	}

	/**
	 * 立即停止所有线程
	 */
	public void shutdownNow() {
		if (null != incomeHandler) {
			incomeHandler.stop();
		}
		if (null != outwardHandler) {
			outwardHandler.stop();
		}
		// if (null != bankLogHandler) {
		// bankLogHandler.stop();
		// }
		if (null != thirdLogHandler) {
			thirdLogHandler.stop();
		}
		if (null != thirdRequestHandler) {
			thirdRequestHandler.stop();
		}
		if (null != outwardTaskAllocateHandler) {
			outwardTaskAllocateHandler.stop();
		}
		if (null != transferAllocateHandler) {
			transferAllocateHandler.stop();
		}
		if (null != sysBalHandler) {
			sysBalHandler.stop();
		}
		if (null != accInvHandler) {
			accInvHandler.stop();
		}
		if (null != outwardTaskTurnHandler) {
			outwardTaskTurnHandler.stop();
		}
		executorService.shutdownNow();
		scheduledExecutorService.shutdownNow();
	}

	/**
	 * 开启业务处理线程
	 */
	public void startHandler() {
		if (null == thirdRequestHandler) {
			thirdRequestHandler = new ThirdRequestHandler();
			scheduledExecutorService.schedule(thirdRequestHandler, 1000, TimeUnit.MILLISECONDS);
		}
		if (null == incomeHandler) {
			incomeHandler = new IncomeHandler();
			scheduledExecutorService.schedule(incomeHandler, 2000, TimeUnit.MILLISECONDS);
		}
		if (null == outwardHandler) {
			outwardHandler = new OutwardHandler();
			scheduledExecutorService.schedule(outwardHandler, 3000, TimeUnit.MILLISECONDS);
		}
		// if (null == bankLogHandler) {
		// bankLogHandler = new BankLogHandler();
		// scheduledExecutorService.schedule(bankLogHandler, 4000,
		// TimeUnit.MILLISECONDS);
		// }
		if (null == thirdLogHandler) {
			thirdLogHandler = new ThirdLogHandler();
			scheduledExecutorService.schedule(thirdLogHandler, 5000, TimeUnit.MILLISECONDS);
		}
		if (null == outwardTaskAllocateHandler) {
			outwardTaskAllocateHandler = new OutwardTaskAllocateHandler();
			scheduledExecutorService.schedule(outwardTaskAllocateHandler, 6000, TimeUnit.MILLISECONDS);
		}
		if (null == transferAllocateHandler) {
			transferAllocateHandler = new TransferAllocateHandler();
			scheduledExecutorService.schedule(transferAllocateHandler, 1000, TimeUnit.MILLISECONDS);
		}
		if (null == sysBalHandler) {
			sysBalHandler = new SysBalHandler();
			scheduledExecutorService.schedule(sysBalHandler, 1000, TimeUnit.MILLISECONDS);
		}
		if (null == accInvHandler) {
			accInvHandler = new AccInvHandler();
			scheduledExecutorService.schedule(accInvHandler, 1000, TimeUnit.MILLISECONDS);
		}
		if (null == outwardTaskTurnHandler) {
			outwardTaskTurnHandler = new OutwardTaskTurnHandler();
			scheduledExecutorService.schedule(outwardTaskTurnHandler, 10000, TimeUnit.MILLISECONDS);
		}
		restartCancelIncomeRequestTask();
		pullOrdersSchedule();
		pullIncomeOrdersSchedule();
		//2019-04-16 盘口支付回调地址定时同步
		handicapNotifyUrlSchedule();
		//2019-04-16 盘口第三方出款（代付）通道定时同步
		handicapDaifuSyncSchedule();
		//2019-04-24 未明确处理结果的代付订单payCore处理结果同步
		daifuInfoPayCoreResultSyncSchedule();
	}

	/**
	 * 重新启动定时任务，取消超时入款请求
	 */
	public void restartCancelIncomeRequestTask() {
		if (null != cancelIncomeRequest) {
			cancelIncomeRequest.cancel(true);
			logger.info("Cancel scheduleAtFixedRate CancelIncomeRequestTask.");
		}
		long period = 2;
		try {
			period = Long.parseLong(MemCacheUtils.getInstance().getSystemProfile()
					.getOrDefault(UserProfileKey.INCOME_LIMIT_REQUEST_CANCEL.getValue(), "2"));
		} catch (Exception e) {
			logger.error("获取系统设置(入款取消时间),异常", e);
		}
		logger.info("Restart CancelIncomeRequestTask, scheduleAtFixedRate: {} (TimeUnit.HOURS) ", period);
		cancelIncomeRequest = getScheduledExecutorService().scheduleAtFixedRate(new CancelIncomeRequestTask(period), 5,
				2 * 60, TimeUnit.SECONDS);
	}

	// 同步出款订单
	public void pullOrdersSchedule() {
		if (null != pullOrdersSchedule) {
			pullOrdersSchedule.cancel(true);
			logger.info("Cancel scheduleAtFixedRate pullOrdersSchedule.");
		}
		pullOrdersSchedule = getScheduledExecutorService().scheduleAtFixedRate(new PullOrdersFromPlatformTask(), 6,
				2 * 60, TimeUnit.SECONDS);
	}

	// 同步入款订单
	public void pullIncomeOrdersSchedule() {
		if (null != pullIncomeOrdersSchedule) {
			pullIncomeOrdersSchedule.cancel(true);
			logger.debug("Cancel scheduleAtFixedRate pullIncomeOrdersSchedule.");
		}
		pullIncomeOrdersSchedule = getScheduledExecutorService().scheduleAtFixedRate(new PullIncomeOrdersTask(), 10,
				10 * 60, TimeUnit.SECONDS);
	}
	
	// 同步盘口支付回调
	public void handicapNotifyUrlSchedule() {
		if (null != handicapNotifyUrlSchedule) {
			handicapNotifyUrlSchedule.cancel(true);
		}
		//盘口支付回调每隔 2 分钟同步一次
		handicapNotifyUrlSchedule = getScheduledExecutorService().scheduleAtFixedRate(new HandicapNotifyUrlTask(), 10,
				2 * 60, TimeUnit.SECONDS);
	}
	
	// 同步盘口第三方出款（代付）通道配置
	public void handicapDaifuSyncSchedule() {
		if (null != handicapDaifuSyncSchedule) {
			handicapDaifuSyncSchedule.cancel(true);
		}
		//盘口支付回调每隔 10 分钟同步一次
		handicapDaifuSyncSchedule = getScheduledExecutorService().scheduleAtFixedRate(new DaifuConfigSyncTask(), 10,
				10 * 60, TimeUnit.SECONDS);
	}
	
	//未明确处理结果的代付订单payCore处理结果同步
	public void daifuInfoPayCoreResultSyncSchedule() {
		if (null != daifuInfoPayCoreResultSyncSchedule) {
			daifuInfoPayCoreResultSyncSchedule.cancel(true);
		}
		//未明确处理结果的代付订单payCore处理结果同步每隔 5 分钟同步一次
		daifuInfoPayCoreResultSyncSchedule = getScheduledExecutorService().scheduleAtFixedRate(new DaifuInfoGetPayCoreResultTask(), 10,
				5 * 60 , TimeUnit.SECONDS);
	}
}