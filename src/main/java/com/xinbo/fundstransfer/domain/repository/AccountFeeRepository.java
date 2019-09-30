/**
 * 
 */
package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.AccountFee;

/**
 * @author Blake
 *
 */
public interface AccountFeeRepository extends BaseRepository<AccountFee, Integer> {

	AccountFee findByAccountId(Integer accountId);

}
