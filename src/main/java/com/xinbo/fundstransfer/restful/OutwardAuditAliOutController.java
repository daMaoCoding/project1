package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.IncomeAuditAliInService;
import com.xinbo.fundstransfer.service.OutwardAuditAliOutService;

/**
 * 支付宝出款请求接口
 * 
 * @author 007
 *
 */
@RestController
@RequestMapping("/r/outAuditAliout")
public class OutwardAuditAliOutController extends BaseController {
	@Autowired
	private OutwardAuditAliOutService outwardAuditAliOutService;
	@Autowired
	private ObjectMapper mapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(OutwardAuditAliOutController.class);
	/**
	 * 获取正在匹配的支付宝出款单
	 * @throws ParseException 
	 */
	@RequestMapping("/aliOutToMatch")
	public String aliOutToMatch(@RequestParam(value = "handicap", required = false) Integer handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "outMember", required = false) String outMember,
			@RequestParam(value = "outOrder", required = false) String outOrder,
			@RequestParam(value = "timeStart", required = false) Long timeStart,
			@RequestParam(value = "timeEnd", required = false) Long timeEnd,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo", required = false) Integer pageNo) throws JsonProcessingException, ParseException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));
		}
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
				Sort.Direction.DESC, "create_time");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Map<String,Object> map =  outwardAuditAliOutService.aliOutToMatch(pageRequest, handicap, level, outMember,
				 outOrder,sdf.format(new Date(timeStart)),sdf.format(new Date(timeEnd)));
		 Page<Object> page = (Page<Object>) map.get("page");
		double totalAmount =  map.get("totalAmount")==null?0d:((BigDecimal) map.get("totalAmount")).doubleValue();
		List<Object> AliLogList = (List<Object>) page.getContent();
		List<Map<String, Object>> arrlist = new ArrayList<Map<String, Object>>();
		Map<String, Object> AliRequest = null;
		 long nowTime = System.currentTimeMillis();
		for (int i = 0; i < AliLogList.size(); i++) {
			Object[] obj = (Object[]) AliLogList.get(i);
			AliRequest = new HashMap<String, Object>();
			AliRequest.put("id", obj[0]);
			AliRequest.put("handicapName", obj[1]);
			AliRequest.put("levelName", obj[2]);
			AliRequest.put("member_user_name", obj[3]);
			AliRequest.put("order_no", obj[4]);
			AliRequest.put("create_time", obj[5]);
			AliRequest.put("amount", obj[6]);
			AliRequest.put("waitLongTime", nowTime-sdf.parse(obj[5].toString()).getTime());
			arrlist.add(AliRequest);
		}
	GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(arrlist);
		responseData.setPage(new Paging(page));
		responseData.setMessage(totalAmount+"");
		return mapper.writeValueAsString(responseData);
		
	}

	/**
	 * 获取失败的支付宝出款单
	 * @throws ParseException 
	 */
	@RequestMapping("/aliOutFail")
	public String aliOutFail(@RequestParam(value = "handicap", required = false) Integer handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "outMember", required = false) String outMember,
			@RequestParam(value = "outOrder", required = false) String outOrder,
			@RequestParam(value = "timeStart", required = false) Long timeStart,
			@RequestParam(value = "timeEnd", required = false) Long timeEnd,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo", required = false) Integer pageNo) throws JsonProcessingException, ParseException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));
		}
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
				Sort.Direction.DESC, "create_time");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Map<String,Object> map =  outwardAuditAliOutService.aliOutFail(pageRequest, handicap, level, outMember,
				 outOrder,sdf.format(new Date(timeStart)),sdf.format(new Date(timeEnd)));
		 Page<Object> page = (Page<Object>) map.get("page");
		double totalAmount =  map.get("totalAmount")==null?0d:((BigDecimal) map.get("totalAmount")).doubleValue();
		List<Object> AliLogList = (List<Object>) page.getContent();
		List<Map<String, Object>> arrlist = new ArrayList<Map<String, Object>>();
		Map<String, Object> AliRequest = null;
		for (int i = 0; i < AliLogList.size(); i++) {
			Object[] obj = (Object[]) AliLogList.get(i);
			AliRequest = new HashMap<String, Object>();
			AliRequest.put("id", obj[0]);
			AliRequest.put("handicapName", obj[1]);
			AliRequest.put("levelName", obj[2]);
			AliRequest.put("member_user_name", obj[3]);
			AliRequest.put("order_no", obj[4]);
			AliRequest.put("create_time", obj[5]);
			AliRequest.put("amount", obj[6]);
			AliRequest.put("waitLongTime", sdf.parse(obj[7].toString()).getTime()-sdf.parse(obj[5].toString()).getTime());
			arrlist.add(AliRequest);
		}
	GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(arrlist);
		responseData.setPage(new Paging(page));
		responseData.setMessage(totalAmount+"");
		return mapper.writeValueAsString(responseData);
		
	}
	
	/**
	 * 获取进行中支付宝出款单
	 * @throws ParseException 
	 */
	@RequestMapping("/aliOutMatched")
	public String aliOutMatched(@RequestParam(value = "handicap", required = false) Integer handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "toMember", required = false) String toMember,
			@RequestParam(value = "inOrderNo", required = false) String inOrderNo,
			@RequestParam(value = "outOrderNo", required = false) String outOrderNo,
			@RequestParam(value = "toHandicapRadio", required = false) Integer toHandicapRadio,
			@RequestParam(value = "timeStart", required = false) Long timeStart,
			@RequestParam(value = "timeEnd", required = false) Long timeEnd,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo", required = false) Integer pageNo) throws JsonProcessingException, ParseException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));
		}
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
				Sort.Direction.DESC, "orderAll.create_time");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Map<String,Object> map =  outwardAuditAliOutService.aliOutMatched(pageRequest, handicap, level, member,
				 toMember,inOrderNo,outOrderNo,toHandicapRadio,sdf.format(new Date(timeStart)),sdf.format(new Date(timeEnd)));
		 Page<Object> page = (Page<Object>) map.get("page");
		double totalAmount =  map.get("totalAmount")==null?0d:((BigDecimal) map.get("totalAmount")).doubleValue();
		double totalToAmount =  map.get("totalToAmount")==null?0d:((BigDecimal) map.get("totalToAmount")).doubleValue();
		List<Object> AliLogList = (List<Object>) page.getContent();
		List<Map<String, Object>> arrlist = new ArrayList<Map<String, Object>>();
		Map<String, Object> AliRequest = null;
		 long nowTime = System.currentTimeMillis();
		for (int i = 0; i < AliLogList.size(); i++) {
			Object[] obj = (Object[]) AliLogList.get(i);
			AliRequest = new HashMap<String, Object>();
			AliRequest.put("id", obj[0]);
			AliRequest.put("handicapName", obj[1]);
			AliRequest.put("levelName", obj[2]);
			AliRequest.put("member", obj[3]);
			AliRequest.put("orderNo", obj[4]);
			AliRequest.put("createTime", obj[6]);
			AliRequest.put("amount", obj[8]);
			AliRequest.put("toMember", obj[9]);
			AliRequest.put("toOrderNo", obj[10]);
			AliRequest.put("toHandicapName", obj[11]);
			AliRequest.put("toLevelName", obj[12]);
			AliRequest.put("toAmount", obj[13]);
			AliRequest.put("toAmountSum", obj[14]);
			AliRequest.put("waitLongTime", nowTime-sdf.parse(obj[7].toString()).getTime());
			arrlist.add(AliRequest);
		}
	GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(arrlist);
		responseData.setPage(new Paging(page));
		responseData.setMessage(totalAmount+":"+totalToAmount);
		return mapper.writeValueAsString(responseData);
		
	}
	
	/**
	 * 获取成功的支付宝出款单
	 * @throws ParseException 
	 */
	@RequestMapping("/aliOutSuccess")
	public String aliOutSuccess(@RequestParam(value = "handicap", required = false) Integer handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "toMember", required = false) String toMember,
			@RequestParam(value = "inOrderNo", required = false) String inOrderNo,
			@RequestParam(value = "outOrderNo", required = false) String outOrderNo,
			@RequestParam(value = "toHandicapRadio", required = false) Integer toHandicapRadio,
			@RequestParam(value = "timeStart", required = false) Long timeStart,
			@RequestParam(value = "timeEnd", required = false) Long timeEnd,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo", required = false) Integer pageNo) throws JsonProcessingException, ParseException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));
		}
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
				Sort.Direction.DESC, "orderAll.create_time");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Map<String,Object> map =  outwardAuditAliOutService.aliOutSuccess(pageRequest, handicap, level,  member,
				 toMember,inOrderNo,outOrderNo,toHandicapRadio,sdf.format(new Date(timeStart)),sdf.format(new Date(timeEnd)));
		 Page<Object> page = (Page<Object>) map.get("page");
		double totalAmount =  map.get("totalAmount")==null?0d:((BigDecimal) map.get("totalAmount")).doubleValue();
		double totalToAmount =  map.get("totalToAmount")==null?0d:((BigDecimal) map.get("totalToAmount")).doubleValue();
		List<Object> AliLogList = (List<Object>) page.getContent();
		List<Map<String, Object>> arrlist = new ArrayList<Map<String, Object>>();
		Map<String, Object> AliRequest = null;
		for (int i = 0; i < AliLogList.size(); i++) {
			Object[] obj = (Object[]) AliLogList.get(i);
			AliRequest = new HashMap<String, Object>();
			AliRequest.put("id", obj[0]);
			AliRequest.put("handicapName", obj[1]);
			AliRequest.put("levelName", obj[2]);
			AliRequest.put("member", obj[3]);
			AliRequest.put("orderNo", obj[4]);
			AliRequest.put("createTime", obj[6]);
			AliRequest.put("finishTime", obj[7]);
			AliRequest.put("amount", obj[8]);
			AliRequest.put("toMember", obj[9]);
			AliRequest.put("toOrderNo", obj[10]);
			AliRequest.put("toHandicapName", obj[11]);
			AliRequest.put("toLevelName", obj[12]);
			AliRequest.put("toAmount", obj[13]);
			AliRequest.put("toAmountSum", obj[14]);
			AliRequest.put("waitLongTime", sdf.parse(obj[7].toString()).getTime()-sdf.parse(obj[6].toString()).getTime());
			arrlist.add(AliRequest);
		}
	GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
	
		responseData.setData(arrlist);
		responseData.setPage(new Paging(page));
		responseData.setMessage(totalAmount+":"+totalToAmount);
		return mapper.writeValueAsString(responseData);
		
	}
}
