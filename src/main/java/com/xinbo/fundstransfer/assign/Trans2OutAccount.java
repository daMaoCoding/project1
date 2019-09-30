package com.xinbo.fundstransfer.assign;

import java.util.Set;

import com.xinbo.fundstransfer.assign.entity.ReserveAccount;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;

public interface Trans2OutAccount {
	AccountBaseInfo getAccountBaseInfo();

	int getAccountId();

	void setAccountId(int accountId);

	int getLevelId();

	void setLevelId(int levelId);

	int getType();

	void setType(int type);

	int getZone();

	void setZone(int zone);

	int getHandicapId();

	void setHandicapId(int handicapId);

	int getAmount();

	void setAmount(int amount);

	boolean isUsed();

	void setUsed(boolean isUsed);

	boolean transReserveWhenTooMuchBalance(ReserveAccount reserveAccount, Set<String> evading, int minBal);
	
	void setRemain(int remain);
	
	void minusRemain(int amount);
	
	int getRemain();
}
