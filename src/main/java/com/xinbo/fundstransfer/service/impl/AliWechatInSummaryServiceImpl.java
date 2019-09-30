package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.xinbo.fundstransfer.domain.pojo.AliInSummaryInputDTO;
import com.xinbo.fundstransfer.service.AliWechatInSummaryService;

/**
 * Created by Administrator on 2018/10/5.
 */
@Service
public class AliWechatInSummaryServiceImpl implements AliWechatInSummaryService {
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Object[]> findPage(AliInSummaryInputDTO inputDTO, List<String> handicaps, List<String> levels) {
		String sql = "select i.id, i.handicap,i.level,i.to_account,i.order_no,i.member_user_name,i.amount,i.create_time,i.update_time,i.remark,(SELECT a.id FROM biz_account a WHERE a.account=i.to_account limit 1) as toAccountId from biz_income_request i where i.status=1 ";
		sql = wrapQuerySqlStr(inputDTO, sql, handicaps, levels);
		List<Object[]> list = entityManager.createNativeQuery(sql)
				.setFirstResult(inputDTO.getPageNo() * inputDTO.getPageSize()).setMaxResults(inputDTO.getPageSize())
				.getResultList();
		return list;
	}

	@Override
	public Long count(AliInSummaryInputDTO inputDTO, List<String> handicaps, List<String> levels) {
		String sql = "SELECT count(1) from biz_income_request i where  i.status=1  ";
		sql = wrapQuerySqlStr(inputDTO, sql, handicaps, levels);
		BigInteger count = (BigInteger) entityManager.createNativeQuery(sql).getSingleResult();
		return count.longValue();
	}

	@Override
	public double sum(AliInSummaryInputDTO inputDTO, List<String> handicaps, List<String> levels) {
		String sql = "SELECT SUM(amount) from biz_income_request i where i.status=1  ";
		sql = wrapQuerySqlStr(inputDTO, sql, handicaps, levels);
		BigDecimal result = (BigDecimal) entityManager.createNativeQuery(sql).getSingleResult();
		result = (null == result ? BigDecimal.ZERO : result);
		return result.doubleValue();
	}

	private String wrapQuerySqlStr(AliInSummaryInputDTO inputDTO, String sql, List<String> handicaps,
			List<String> levels) {
		sql += " and i.type= " + inputDTO.getType();
		if (!CollectionUtils.isEmpty(handicaps)) {
			if (handicaps.size() == 1) {
				sql += " and i.handicap=\'" + handicaps.get(0) + "\'";
			} else {
				sql += " and i.handicap in(";
				for (int i = 0, size = handicaps.size(); i < size; i++) {
					if (i < size - 1) {
						sql += "\'" + handicaps.get(i) + "\',";
					} else {
						sql += "\'" + handicaps.get(i) + "\')";
					}
				}
			}

		}
		if (StringUtils.isNotBlank(inputDTO.getLevel())) {
			sql += " and i.level=\'" + inputDTO.getLevel() + "\'";
		}
		// if (!CollectionUtils.isEmpty(levels)) {
		// if (levels.size() == 1) {
		// sql += " and level=" + levels.get(0);
		// } else {
		// sql += " and level in(";
		// for (int i = 0, size = levels.size(); i < size; i++) {
		// if (i < size - 1) {
		// sql += levels.get(i) + ",";
		// } else {
		// sql += levels.get(i) + ")";
		// }
		// }
		// }
		//
		// }
		if (StringUtils.isNotBlank(inputDTO.getAliAccount())) {
			sql += " and i.to_account=\'" + inputDTO.getAliAccount() + "\'";
		}
		if (StringUtils.isNotBlank(inputDTO.getOrderNo())) {
			sql += " and i.order_no=\'" + inputDTO.getOrderNo() + "\'";
		}
		if (StringUtils.isNotBlank(inputDTO.getMember())) {
			sql += " and i.member_user_name=\'" + inputDTO.getMember() + "\'";
		}
		if (inputDTO.getFromMoney() != null) {
			sql += " and i.amount >=" + inputDTO.getFromMoney();
		}
		if (inputDTO.getToMoney() != null) {
			sql += " and i.amount <=" + inputDTO.getToMoney();
		}
		if (StringUtils.isNotBlank(inputDTO.getStartTime())) {
			sql += " and i.create_time >=\'" + inputDTO.getStartTime() + "\'";
		}
		if (StringUtils.isNotBlank(inputDTO.getEndTime())) {
			sql += " and i.create_time <=\'" + inputDTO.getEndTime() + "\'";
		}
		sql += " order by i.create_time desc";
		return sql;
	}
}
