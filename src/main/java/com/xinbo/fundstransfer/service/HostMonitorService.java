package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.component.net.socket.AccountEntity;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.domain.entity.BizAccount;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface HostMonitorService {

	List<Map<String, Object>> list(String host, String accountLike, Integer[] statusArray);

	void shutdown(String host);

	void updateHostType(String host, Integer accountType, Integer currSysLevel);

	/**
	 * 获取当前host等待时间
	 */
	MessageEntity<List<AccountEntity>> getMessageEntity(String host);

	void upgradeByCommand();

	void startByCommand(int accountId);

	void stopByCommand(int accountId);

	void pauseByCommand(int accountId);

	void resumeByCommand(int accountId);

	void changeMode(int accountId, ActionEventEnum command);

	void addAccountToHost(String host, int accountId);

	void removeAccountFromHost(String host, int accountId);

	void alterSignAndHook(String host, int accountId, String sign, String hook, String hub, String bing,
			Integer interval) throws Exception;

	void alterSignAndHook(int accountId, String sign, String hook, String hub, String bing) throws Exception;

	void updateIinterval(String host, int accountId, Integer interval) throws Exception;

	void update(BizAccount bizAccount);

	List<AccountEntity> findAccountEntityList(String host);

	List<Integer> findAllAccountIdList();

	void messageBroadCast(MessageEntity o);

	String findHostByAcc(Integer accId);
}
