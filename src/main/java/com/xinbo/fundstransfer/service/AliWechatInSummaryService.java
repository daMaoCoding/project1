package com.xinbo.fundstransfer.service;

import java.util.List;

import com.xinbo.fundstransfer.domain.pojo.AliInSummaryInputDTO;

/**
 * Created by Administrator on 2018/10/5.
 */
public interface AliWechatInSummaryService {
	List<Object[]> findPage(AliInSummaryInputDTO inputDTO, List<String> handicaps, List<String> levels);

	Long count(AliInSummaryInputDTO inputDTO, List<String> handicaps, List<String> levels);

	double sum(AliInSummaryInputDTO inputDTO, List<String> handicaps, List<String> levels);
}
