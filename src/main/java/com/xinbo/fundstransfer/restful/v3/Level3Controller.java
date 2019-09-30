package com.xinbo.fundstransfer.restful.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3Level;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class Level3Controller extends Base3Common {
	private static final Logger log = LoggerFactory.getLogger(Level3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private LevelService levelService;

	@RequestMapping(value = "/findLevel", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public Map<String, Object> limit(@Valid @RequestBody ReqV3Level requestBody, BindingResult result)
			throws JsonProcessingException {
		String paramBody = mapper.writeValueAsString(requestBody);
		log.info("Level3 >> RequestBody:{}", paramBody);
		if (result.hasErrors()) {
			log.debug("Level3 >> invalid params. RequestBody:{}", paramBody);
			return SimpleResponseData.getErrorMapResult("参数验证失败");
		}
		if (!checkToken(requestBody)) {
			log.debug("Level3 >> token验证失败. RequestBody:{}", paramBody);
			return SimpleResponseData.getErrorMapResult("token验证失败");
		}
		try {
			BizLevel level = levelService.findFromCache(Integer.parseInt(requestBody.getOid()),
					requestBody.getLevelCode());
			if (null != level && null != level.getCurrSysLevel()) {
				Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
				successMapResult.put("currSysLevel", level.getCurrSysLevel());
				return successMapResult;
			} else {
				log.debug("Level3 >> 查询不到数据参数. RequestBody:{}", paramBody);
				return SimpleResponseData.getErrorMapResult(
						"查询不到数据参数 Oid:" + requestBody.getOid() + "Code:" + requestBody.getLevelCode());
			}
		} catch (Exception e) {
			log.error("Level3>>,出错:{}", e.getMessage(), e);
			return SimpleResponseData.getErrorMapResult(
					StringUtils.isEmpty(e.getMessage()) ? "内部错误," + System.currentTimeMillis() : e.getMessage());
		}
	}

	/**
	 * check whether token is valid.
	 * <p>
	 * calculate target : amount+acc+logid+tid+salt.
	 *
	 * @return {@code true} pass checkpoint {@code false } non-pass checkpoint
	 */
	private boolean checkToken(ReqV3Level arg0) {
		if (StringUtils.isBlank(arg0.getToken())) {
			log.debug("Level3 >> param token is empty|null. oid: {} levelCode: {} levelName: {}", arg0.getOid(),
					arg0.getLevelCode(), arg0.getLevelName());
			return false;
		}
		String oriContent = arg0.getOid() + arg0.getLevelCode() + arg0.getLevelName();
		String calToken = CommonUtils.md5digest(oriContent + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, arg0.getToken()))
			return true;
		log.debug("Level3 >> param token is empty|null. oid: {} levelCode: {} levelName: {}", arg0.getOid(),
				arg0.getLevelCode(), arg0.getLevelName());
		return false;
	}
}
