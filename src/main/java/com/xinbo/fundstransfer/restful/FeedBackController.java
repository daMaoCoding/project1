package com.xinbo.fundstransfer.restful;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.util.Base64;
import org.apache.shiro.SecurityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.ali4enterprise.inputdto.CommonInputDTO;
import com.xinbo.fundstransfer.ali4enterprise.outputdto.PageOutPutDTO;
import com.xinbo.fundstransfer.component.common.BASE64DecodedMultipartFile;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.entity.BizFeedBack;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.FeedBackLevel;
import com.xinbo.fundstransfer.domain.enums.FeedBackStatus;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.service.FeedBackService;
import com.xinbo.fundstransfer.service.SysUserService;

/**
 * 意见反馈
 *
 * @author 007
 */
@Slf4j
@RestController
@RequestMapping("/r/feedback")
public class FeedBackController extends BaseController {
	@Autowired
	private FeedBackService feedBackService;
	@Autowired
	private SysUserService sysUserService;
	@Value("${funds.transfer.multipart.location}")
	private String uploadPathNew;

	@RequestMapping("/findFeedBack")
	public String findFeedBack(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "untreatedFind", required = false) String untreatedFind,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "descOrAsc", required = false) String descOrAsc,
			@RequestParam(value = "orderBy", required = false) String orderBy,
			@RequestParam(value = "processedDescOrAsc", required = false) String processedDescOrAsc,
			@RequestParam(value = "processedOrderBy", required = false) String processedOrderBy,
			@RequestParam(value = "timeDescOrAsc", required = false) String timeDescOrAsc,
			@RequestParam(value = "timeOrderBy", required = false) String timeOrderBy,
			@RequestParam(value = "level", required = false) String level,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					(timeDescOrAsc != null && !timeDescOrAsc.equals(""))
							? (timeDescOrAsc.equals("desc") ? Sort.Direction.ASC : Sort.Direction.DESC)
							: (type.equals("untreatedFind")
									? ((descOrAsc != null && descOrAsc.equals("desc")) ? Sort.Direction.ASC
											: Sort.Direction.DESC)
									: ((processedDescOrAsc != null && processedDescOrAsc.equals("desc"))
											? Sort.Direction.ASC
											: Sort.Direction.DESC)),
					(timeOrderBy != null && !timeOrderBy.equals("")) ? "create_time"
							: (type.equals("untreatedFind")
									? ((orderBy != null && orderBy.equals("level")) ? "level" : "create_time")
									: ((processedOrderBy != null && processedOrderBy.equals("level")) ? "level"
											: "update_time")));
			// 拼接时间戳查询数据条件
			String fristTime = null;
			String lastTime = null;
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
			}
			untreatedFind = !"".equals(untreatedFind) ? untreatedFind : null;
			Map<String, Object> mapp = feedBackService.findFeedBack(untreatedFind, fristTime, lastTime, type, level,
					pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> feedBackList = (List<Object>) page.getContent();
			List<BizFeedBack> arrlist = new ArrayList<BizFeedBack>();
			for (int i = 0; i < feedBackList.size(); i++) {
				Object[] obj = (Object[]) feedBackList.get(i);
				SysUser creator = sysUserService.findFromCacheById(Integer.valueOf(obj[6].toString()));
				SysUser acceptor = sysUserService
						.findFromCacheById(obj[7] == null ? 0 : Integer.valueOf(obj[7].toString()));
				BizFeedBack bizFeedBack = new BizFeedBack();
				bizFeedBack.setId((int) obj[0]);
				bizFeedBack.setCreateTime((String) obj[1]);
				bizFeedBack.setUpdateTime((String) obj[2]);
				bizFeedBack.setLevel(FeedBackLevel.findByStatus((int) obj[3]).getMsg());
				bizFeedBack.setStatus(FeedBackStatus.findByStatus((int) obj[4]).getMsg());
				bizFeedBack.setIssue(StringUtils.isNotBlank((String) obj[5])
						? ((String) obj[5]).replace("\r\n", "<br>").replace("\n", "<br>")
						: "");
				bizFeedBack.setCreator(creator == null ? "" : creator.getUid());
				bizFeedBack.setAcceptor(acceptor == null ? "" : acceptor.getUid());
				bizFeedBack.setImgs((String) obj[8]);
				arrlist.add(bizFeedBack);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用意见反馈Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/saveFeedBack")
	public String saveFeedBack(@RequestParam(value = "images", required = false) String images,
			@RequestParam(value = "level", required = false) String level,
			@RequestParam(value = "describe", required = false) String describe) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			JSONArray jsonArray = new JSONArray(images.toString());
			String imgs = "";
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");// 设置日期格式
			SimpleDateFormat picDf = new SimpleDateFormat("yyyyMMddHHmmss");// 设置日期格式
			String nowTime = df.format(new Date());
			for (int i = 0; i < jsonArray.length(); i++) {
				String picNowTime = picDf.format(new Date());
				JSONObject jsonObj = jsonArray.getJSONObject(i);
				BizFeedBack bizFeedBack = mapper.readValue(jsonObj.toString(), BizFeedBack.class);
				String[] baseStrs = bizFeedBack.getBase64().split(",");
				byte[] bytes = Base64.decodeBase64(baseStrs[1].getBytes());
				MultipartFile file = new BASE64DecodedMultipartFile(bytes,
						operator.getUid() + picNowTime + i + bizFeedBack.getName().split("\\.")[1]);
				String absolutePath = uploadPathNew + "/feedBack/" + nowTime + "/";
				// 判断文件是否存在
				File dir = new File(absolutePath);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				absolutePath += operator.getUid() + picNowTime + i + bizFeedBack.getName().split("\\.")[1];
				log.info("File saved, absolutePath:{}", absolutePath);
				file.transferTo(new File(absolutePath));
				imgs += "/feedBack/" + nowTime + "/" + operator.getUid() + picNowTime + i
						+ bizFeedBack.getName().split("\\.")[1] + ",";
			}
			feedBackService.saveFeedBack(level, CommonUtils.genRemark("", describe, new Date(), operator.getUid()),
					operator.getId().toString(), imgs);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用意见反馈Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/saveRemark")
	public String saveRemark(@RequestParam(value = "id", required = false) String id,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "type", required = false) String type) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			// 查找旧的备注
			String oldRmark = feedBackService.findOldRemark(id);
			if ("remark".equals(type)) {
				feedBackService.saveRemark(id,
						CommonUtils.genRemark(oldRmark, remark + "(备注)", new Date(), operator.getUid()));
			} else if ("cl".equals(type) || "return".equals(type)) {
				feedBackService.dealWith(id, operator.getId().toString(), CommonUtils.genRemark(oldRmark,
						remark + ("cl".equals(type) ? "(处理)" : "(驳回)"), new Date(), operator.getUid()));
			} else if ("delete".equals(type)) {
				feedBackService.deleteFeedBack(id);
			} else {
				feedBackService.finish(id);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用意见反馈Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/showFeedBackDetails")
	public String showFeedBackDetails(@RequestParam(value = "id", required = false) String id)
			throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<Object> detailList = feedBackService.showFeedBackDetails(id);
			List<BizFeedBack> arrlist = new ArrayList<BizFeedBack>();
			for (int i = 0; i < detailList.size(); i++) {
				Object[] obj = (Object[]) detailList.get(i);
				SysUser creator = sysUserService.findFromCacheById(Integer.valueOf(obj[6].toString()));
				SysUser acceptor = sysUserService
						.findFromCacheById(obj[7] == null ? 0 : Integer.valueOf(obj[7].toString()));
				BizFeedBack bizFeedBack = new BizFeedBack();
				bizFeedBack.setId((int) obj[0]);
				bizFeedBack.setCreateTime((String) obj[1]);
				bizFeedBack.setUpdateTime((String) obj[2]);
				bizFeedBack.setLevel(FeedBackLevel.findByStatus((int) obj[3]).getMsg());
				bizFeedBack.setStatus(FeedBackStatus.findByStatus((int) obj[4]).getMsg());
				bizFeedBack.setIssue(StringUtils.isNotBlank((String) obj[5])
						? ((String) obj[5]).replace("\r\n", "<br>").replace("\n", "<br>")
						: "");
				bizFeedBack.setCreator(creator.getUid());
				bizFeedBack.setAcceptor(acceptor == null ? "" : acceptor.getUid());
				bizFeedBack.setImgs((String) obj[8]);
				arrlist.add(bizFeedBack);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用意见反馈Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/deleteImgs")
	public void deleteImgs(@RequestParam(value = "imgs", required = false) String imgs) throws JsonProcessingException {
		redisService.convertAndSend(RedisTopics.DELETED_FEEDBACK_SCREENSHOTS, imgs);
	}

	/**
	 * 1.6.5 新增反馈
	 *
	 * @param inputDTO
	 * @param result
	 * @return
	 */
	@RequestMapping("/addForCrk")
	public ResponseDataNewPay addForCrk(@Valid @RequestBody AddForCrkInputDTO inputDTO, BindingResult result) {
		try {
			if (result.hasErrors()) {
				return new ResponseDataNewPay((byte) -1, "本系统参数校验不通过!");
			}
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return new ResponseDataNewPay((byte) -1, " 重新登陆");
			}
			inputDTO.setAdminId(sysUser.getId().longValue());
			inputDTO.setUserName(sysUser.getUid());
			ResponseDataNewPay rs = feedBackService.addForCrk(inputDTO);
			if (rs.getCode() != 200) {
				return new ResponseDataNewPay((byte) -1, "失败:" + rs.getMsg());
			}
			rs.setStatus((byte) 1);
			return rs;
		} catch (Exception e) {
			log.error("addForCrk error:", e);
			return new ResponseDataNewPay((byte) -1, "操作失败:" + e.getLocalizedMessage());
		}
	}

	/**
	 * 1.6.6 上传文件
	 *
	 * @param
	 * @return
	 */
	@RequestMapping(value = "/upload")
	public ResponseDataNewPay upload(MultipartRequest request) {
		List<File> tempFile = null;
		try {
			List<MultipartFile> list = request.getFiles("files");
			if (CollectionUtils.isEmpty(list)) {
				return new ResponseDataNewPay((byte) -1, "请选择上传的文件!");
			}
			tempFile = new ArrayList<>();
			final String[] illegalFormat = { "exe", "bat", "sh" };
			for (int i = 0, len = list.size(); i < len; i++) {
				String name = list.get(i).getOriginalFilename();
				String type = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
				if (StringUtils.isBlank(type) || Arrays.asList(illegalFormat).contains(type)) {
					return new ResponseDataNewPay((byte) -1, "文件:" + name + "格式不允许上传!");
				}
				if (list.get(i).getSize() > 2 * 1024 * 1024) {
					return new ResponseDataNewPay((byte) -1, "文件:" + name + "太大,要求小于2M!");
				}
				String projectPath = System.getProperty("user.dir");
				if (StringUtils.isBlank(projectPath)) {
					return null;
				}
				InputStream ins = list.get(i).getInputStream();
				File temFile = new File(projectPath + "/data/tmp/" + list.get(i).getOriginalFilename());
				inputStreamToFile(ins, temFile);
				tempFile.add(temFile);
			}
			// ResponseDataNewPay res = feedBackService.upload(tempFile);
			ResponseDataNewPay res = feedBackService.upload2(tempFile);
			if (res == null || res.getCode() != 200) {
				return new ResponseDataNewPay((byte) -1, res == null ? "上传失败" : "上传失败:" + res.getMsg());
			}
			if (!CollectionUtils.isEmpty(tempFile)) {
				tempFile.stream().forEach(p -> {
					if (p.exists())
						p.delete();
				});
			}
			if (Objects.nonNull(res.getData())) {
				Map resMap = (Map) res.getData();
				List<String> data = new ArrayList<>();
				for (Object map : resMap.values()) {
					data.add(map.toString());
				}
				res.setData(data);
			}
			res.setStatus((byte) 1);
			return res;
		} catch (Exception e) {
			if (!CollectionUtils.isEmpty(tempFile)) {
				tempFile.stream().forEach(p -> {
					if (p.exists())
						p.delete();
				});
			}
			log.error("upload error:", e);
			return new ResponseDataNewPay<>((byte) -1, "上传失败:" + e.getLocalizedMessage());
		}
	}

	private final void inputStreamToFile(InputStream ins, File file) {
		try {
			OutputStream os = new FileOutputStream(file);
			int bytesRead;
			byte[] buffer = new byte[8192];
			while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			os.close();
			ins.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ResponseDataNewPay wrapRes(ResponseDataNewPay res) {
		if (Objects.isNull(res)) {
			return new ResponseDataNewPay((byte) -1, "操作失败!");
		}
		if (res.getCode() != 200) {
			res.setStatus((byte) -1);
			res.setMsg("操作失败!");
			return res;
		}
		res.setStatus((byte) 1);
		res.setMsg("操作成功!");
		return res;
	}

	/**
	 * 1.1.1 查询反馈列表
	 *
	 * @param inputDTO
	 *            {@link FindForOidInputDTO}
	 * @param result
	 *            {@link ResponseDataNewPay<PageOutPutDTO<FindForOidOutputDTO>>}
	 * @return
	 */
	@PostMapping(value = "/findForOid")
	public ResponseDataNewPay<PageOutPutDTO<FindForOidOutputDTO>> findForOid(
			@Valid @RequestBody FindForOidInputDTO inputDTO, BindingResult result) {
		try {
			if (result.hasErrors()) {
				return new ResponseDataNewPay((byte) -1, "本系统参数校验失败!");
			}
			ResponseDataNewPay<PageOutPutDTO<FindForOidOutputDTO>> res = feedBackService.findForOid(inputDTO);
			return wrapRes(res);
		} catch (Exception e) {
			log.error("查询反馈列表失败:", e);
		}
		return null;
	}

	/**
	 * 1.1.2 业主反馈 查看反馈内容
	 *
	 * @param inputDTO
	 *            {@link CommonInputDTO}
	 * @return {@link ResponseDataNewPay<PageOutPutDTO<FindContentOutputDTO>>}
	 */
	@GetMapping("/findContent")
	public ResponseDataNewPay<PageOutPutDTO<FindContentOutputDTO>> findContent(CommonInputDTO inputDTO) {
		try {
			ResponseDataNewPay<PageOutPutDTO<FindContentOutputDTO>> res = feedBackService.findContent(inputDTO);
			return wrapRes(res);
		} catch (Exception e) {
			log.error("业主反馈查看反馈内容失败:", e);
		}
		return null;
	}

	/**
	 * 1.1.3 查询回复记录
	 *
	 * @param inputDTO
	 *            {@link FindForOfbInputDTO}
	 * @param result
	 *            {@link ResponseDataNewPay<PageOutPutDTO<FindForOfbOutputDTO>>}
	 * @return
	 */
	@PostMapping("/findForOfb")
	public ResponseDataNewPay<PageOutPutDTO<FindForOfbOutputDTO>> findForOfb(
			@Valid @RequestBody FindForOfbInputDTO inputDTO, BindingResult result) {
		try {
			if (result.hasErrors()) {
				return new ResponseDataNewPay((byte) -1, "本系统参数校验失败!");
			}
			ResponseDataNewPay<PageOutPutDTO<FindForOfbOutputDTO>> res = feedBackService.findForOfb(inputDTO);
			return wrapRes(res);
		} catch (Exception e) {
			log.error("查询回复记录失败:", e);
		}
		return null;
	}

	/**
	 * 1.1.4 查询指定业主反馈的需求进度
	 *
	 * @param inputDTO
	 *            {@link FindForOfb4DemandInputDTO }
	 * @param result
	 *            {@link ResponseDataNewPay<PageOutPutDTO<FindForOfb4DemandOutputDTO>>
	 *            }
	 * @return
	 */
	@PostMapping("/findForOfb4Demand")
	public ResponseDataNewPay<PageOutPutDTO<FindForOfb4DemandOutputDTO>> findForOfb4Demand(
			@Valid @RequestBody FindForOfb4DemandInputDTO inputDTO, BindingResult result) {
		try {
			if (result.hasErrors()) {
				return new ResponseDataNewPay((byte) -1, "本系统参数校验失败!");
			}
			ResponseDataNewPay<PageOutPutDTO<FindForOfb4DemandOutputDTO>> res = feedBackService.findForOfb2(inputDTO);
			return wrapRes(res);
		} catch (Exception e) {
			log.error(" 查询指定业主反馈的需求进度失败:", e);
		}
		return null;
	}

	/**
	 * 1.1.5 标记反馈信息已解决
	 *
	 * @param inputDTO
	 *            {@link CommonInputDTO}
	 * @param result
	 *            {@link ResponseDataNewPay}
	 * @return
	 */
	@PostMapping("/solve")
	public ResponseDataNewPay solve(@Valid @RequestBody CommonInputDTO inputDTO, BindingResult result) {
		try {
			if (result.hasErrors()) {
				return new ResponseDataNewPay((byte) -1, "本系统参数校验失败!");
			}
			ResponseDataNewPay res = feedBackService.solve(inputDTO);
			return wrapRes(res);
		} catch (Exception e) {
			log.error(" 标记反馈信息已解决失败:", e);
		}
		return null;
	}
}
