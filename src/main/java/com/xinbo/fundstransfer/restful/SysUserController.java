package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.xinbo.fundstransfer.component.redis.RedisTopics;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.codec.SaltPassword;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.SearchFilter.Operator;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.entity.SysUserRole;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.UserCategory;
import com.xinbo.fundstransfer.domain.pojo.UserCategoryStat;
import com.xinbo.fundstransfer.service.*;

/**
 * 系统管理类 :用户管理 角色管理 系统设置
 * shiro各操作权限键定义CRUD:新增(Create)、读取查询(Retrieve)、更新(Update)、删除(Delete)，其它功能菜单自定义
 */
@RestController
@RequestMapping("/r/user")
public class SysUserController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(SysUserController.class);
	@Autowired
	private HandicapService handicapService;
	@Autowired
	public HttpServletRequest request;
	@Autowired
	private SysUserService userService;
	@Autowired
	private SysUserProfileService userProfileService;
	@Autowired
	private SysRoleService roleService;
	@Autowired
	private SysUserRoleService userRoleService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private LevelService levelService;
	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping("/refreshCache")
	public String refreshCache(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		try {
			redisService.convertAndSend(RedisTopics.REFRESH_USER, String.valueOf(id));
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(), "清理缓存成功"));
		} catch (Exception e) {
			logger.error("清理缓存失败", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "清理缓存失败"));
		}
	}

	@RequestMapping("/getOutWardTaskUser")
	public String getOutWardTaskUser() throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(operator)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			GeneralResponseData<List<SysUser>> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			List<SysUser> dataList = userService.findOutWardTaskUser();
			int admin = com.xinbo.fundstransfer.domain.enums.UserCategory.ADMIN.getValue();
			if (!CollectionUtils.isEmpty(dataList) && !Objects.equals(admin, operator.getCategory())) {
				int oprZone = handicapService.findZoneByHandiId(operator.getHandicap());
				Iterator<SysUser> iterator = dataList.iterator();
				while (iterator.hasNext()) {
					SysUser data = iterator.next();
					if (!Objects.equals(oprZone, handicapService.findZoneByHandiId(data.getHandicap())))
						iterator.remove();
				}
			}
			responseData.setData(dataList);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("查询出款人员失败:{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "查询出款人员失败"));
		}
	}

	@RequiresPermissions("SystemUser:Retrieve")
	@RequestMapping("/findUserCategory")
	public String findUserCategory() throws JsonProcessingException {
		try {
			GeneralResponseData<Map<Integer, String>> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			responseData.setData(userService.findCategory(sysUser));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("查询用户分类:{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "查询用户分类"));
		}
	}

	@RequiresPermissions("SystemUser:*")
	@RequestMapping("/findUserCategoryInfo")
	public String findUserCategoryInfo(@RequestParam(value = "userNameLike", required = false) String userNameLike)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<UserCategoryStat>> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null || sysUser.getHandicap() == null) {
				return mapper.writeValueAsString(new GeneralResponseData(ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			List<UserCategoryStat> data = userService.findUserCategoryInfo(sysUser, userNameLike);
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("查询用户分类统计信息:{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "查询用户分类统计信息"));
		}
	}

	@RequiresPermissions("SystemUser:*")
	@RequestMapping("/list")
	public String list(@RequestParam(value = "pageNo", defaultValue = "0", required = false) int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "roleId", required = false) Integer roleId,
			@RequestParam(value = "categoryCode") Integer categoryCode) throws JsonProcessingException {
		try {
			logger.debug("获取用户信息：参数roleId{}", roleId);
			List<SearchFilter> filterList = DynamicSpecifications.build(request);
			GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (roleId != null) {
				List<Integer> userIdList = new ArrayList<>();
				Specification<SysUserRole> specification = DynamicSpecifications.build(SysUserRole.class,
						new SearchFilter("roleId", Operator.EQ, roleId));
				userRoleService.findAll(specification).forEach((p) -> userIdList.add(p.getUserId()));
				if (CollectionUtils.isEmpty(userIdList)) {
					return mapper.writeValueAsString(responseData);
				} else {
					filterList.add(new SearchFilter("id", Operator.IN, userIdList.toArray()));
				}
			}
			if (categoryCode.equals(UserCategory.Outward.getCode())) {
				filterList.add(new SearchFilter("category", Operator.LTE, categoryCode));
			} else {
				filterList.add(new SearchFilter("category", Operator.EQ, categoryCode));
			}
			filterList.add(new SearchFilter("id", Operator.NOTEQ, AppConstants.USER_ID_4_ADMIN));
			if (sysUser.getCategory() != -1) {
				filterList.add(new SearchFilter("category", Operator.NOTEQ, AppConstants.USER_ID_1_ADMIN));
			}
			Specification<SysUser> specif = DynamicSpecifications.build(SysUser.class,
					filterList.toArray(new SearchFilter[filterList.size()]));
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "status", "id");
			Page<SysUser> page = userService.findPage(specif, pageRequest);
			List<Map<String, Object>> dataList = new ArrayList<>();
			responseData.setData(dataList);
			List<Integer> userIdList = new ArrayList<>();
			page.getContent().forEach((p) -> userIdList.add(p.getId()));
			List<SearchFilter> profilterList = new ArrayList<>();
			if (!CollectionUtils.isEmpty(userIdList)) {
				profilterList.add(new SearchFilter("userId", Operator.IN, userIdList.toArray()));
			}
			profilterList.add(new SearchFilter("propertyKey", Operator.IN, new String[] {
					UserProfileKey.OUTDRAW_MONEYLIMIT.getValue(), UserProfileKey.INCOME_AUDITLIMIT.getValue() }));
			Specification<SysUserProfile> proSpecif = DynamicSpecifications.build(SysUserProfile.class,
					profilterList.toArray(new SearchFilter[profilterList.size()]));
			Map<String, SysUserProfile> proMap = new HashMap<>();
			userProfileService.findAll(proSpecif).forEach((p) -> proMap.put((p.getUserId() + p.getPropertyKey()), p));
			List<SearchFilter> rolefilterList = new ArrayList<>();
			if (!CollectionUtils.isEmpty(userIdList)) {
				rolefilterList.add(new SearchFilter("userId", Operator.IN, userIdList.toArray()));
			}
			Map<Integer, String> userRoleMap = new HashMap<>();
			Map<Integer, String> roleMap = new HashMap<>();
			roleService.findList().forEach((p) -> roleMap.put(p.getId(), p.getName()));
			List<SysUserRole> userRoleList = userRoleService.findAll(DynamicSpecifications.build(SysUserRole.class,
					rolefilterList.toArray(new SearchFilter[rolefilterList.size()])));
			userRoleList.forEach((p) -> {
				String r = userRoleMap.get(p.getUserId());
				String n = roleMap.get(p.getRoleId());
				userRoleMap.put(p.getUserId(), StringUtils.isBlank(r) ? n : (r + "|" + n));
			});
			for (SysUser user : page.getContent()) {
				Map<String, Object> item = transUserToMap(user);
				item.put("role", userRoleMap.get(user.getId()));
				SysUserProfile moneyLimit = proMap.get(user.getId() + UserProfileKey.OUTDRAW_MONEYLIMIT.getValue());
				item.put("moneyLimit", moneyLimit != null && moneyLimit.getPropertyValue() != null
						? moneyLimit.getPropertyValue() : StringUtils.EMPTY);
				SysUserProfile auditLimit = proMap.get(user.getId() + UserProfileKey.INCOME_AUDITLIMIT.getValue());
				item.put("auditLimit", auditLimit != null && auditLimit.getPropertyValue() != null
						? new BigDecimal(auditLimit.getPropertyValue()) : StringUtils.EMPTY);
				dataList.add(item);
			}
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("获取失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(ResponseStatus.FAIL.getValue(), "查询失败  "));
		}
	}

	@RequiresPermissions("SystemUser:Retrieve")
	@RequestMapping("/findById")
	public String findById(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		try {
			logger.debug("获取用户信息：参数id{}", id);
			SysUser user = userService.findFromCacheById(id);
			Map<String, SysUserProfile> proMap = new HashMap<>();
			List<SearchFilter> profilterList = new ArrayList<>();
			profilterList.add(new SearchFilter("userId", Operator.EQ, id));
			profilterList.add(new SearchFilter("propertyKey", Operator.IN, new String[] {
					UserProfileKey.OUTDRAW_MONEYLIMIT.getValue(), UserProfileKey.INCOME_AUDITLIMIT.getValue() }));
			Specification<SysUserProfile> proSpecif = DynamicSpecifications.build(SysUserProfile.class,
					profilterList.toArray(new SearchFilter[profilterList.size()]));
			userProfileService.findAll(proSpecif).forEach((p) -> proMap.put((p.getUserId() + p.getPropertyKey()), p));
			Map<String, Object> result = transUserToMap(user);
			SysUserProfile moneyLimit = proMap.get(user.getId() + UserProfileKey.OUTDRAW_MONEYLIMIT.getValue());
			result.put("moneyLimit", moneyLimit != null && moneyLimit.getPropertyValue() != null
					? moneyLimit.getPropertyValue() : StringUtils.EMPTY);
			SysUserProfile auditLimit = proMap.get(user.getId() + UserProfileKey.INCOME_AUDITLIMIT.getValue());
			result.put("auditLimit", auditLimit != null && auditLimit.getPropertyValue() != null
					? new BigDecimal(auditLimit.getPropertyValue()) : StringUtils.EMPTY);
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			responseData.setData(result);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("获取失败：{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData(ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findByUId")
	public String findByUId(@RequestParam(value = "UId") String UId) throws JsonProcessingException {
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(sysUser)) {
				return mapper.writeValueAsString(new GeneralResponseData(ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			Map<String, Object> ret = new HashMap<>();
			if (Objects.nonNull(UId)) {
				SysUser user = userService.findByUid(UId);
				if (Objects.nonNull(user)) {
					ret = transUserToMap(user);
				}
			}
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			responseData.setData(ret);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData(ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequiresPermissions("SystemUser:Update")
	@RequestMapping("/update")
	public String update(@Valid SysUser userVo, @RequestParam(value = "moneyLimit") BigDecimal moneyLimit,
			@RequestParam(value = "auditLimit") BigDecimal auditLimit,
			@RequestParam(value = "resetPassword") int resetPassword) throws Exception {
		try {
			logger.debug("更新参数：userVo{},moneyLimit{}，auditLimit{}，resetPassword{}", userVo, moneyLimit, auditLimit,
					resetPassword);
			if (resetPassword == 1) {
				userVo.setPassword(AppConstants.DEFAULT_PASSWORD);
			}
			SysUser userInDb = StringUtils.isBlank(userVo.getUid()) ? null : userService.findByUid(userVo.getUid());
			if (userVo.getId() == null && StringUtils.isBlank(userVo.getUid())) {
				throw new Exception("账号不能为空！");
			}
			if (userVo.getId() == null && StringUtils.isBlank(userVo.getUsername())) {
				throw new Exception("姓名不能为空！");
			}
			if (userVo.getId() == null && StringUtils.isBlank(userVo.getPassword())) {
				throw new Exception("密码不能为空！");
			}
			if ((userVo.getId() == null && userInDb != null)
					|| (userVo.getId() != null && userInDb != null && !userInDb.getId().equals(userVo.getId()))) {
				throw new Exception("账号已存在，请重新输入！");
			}
			userInDb = userVo.getId() != null ? userService.findFromCacheById(userVo.getId()) : null;
			if (userVo.getId() != null && userInDb == null) {
				throw new Exception("该账号不存在！");
			}
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null || sysUser.getHandicap() == null) {
				throw new Exception("请重新登陆！");
			}
			userVo.setUsername(userVo.getUsername().replace(" ", ""));
			userVo.setUid(userVo.getUid().replace(" ", ""));
			SysUser user = userService.saveOrUpdate(sysUser, resetPassword, moneyLimit, auditLimit, userVo, userInDb);
			// 新增用户时候 默认添加对应的盘口、层级权限。
			if (userInDb == null && userVo.getCategory() > 400) {
				String handicapIds = userVo.getCategory().toString().substring(1,
						userVo.getCategory().toString().length());
				String levelIds = "";
				List<BizLevel> levels = levelService.findByHandicapId(Integer.valueOf(handicapIds));
				for (BizLevel level : levels) {
					levelIds += level.getId() + ",";
				}
				sysDataPermissionService.savePermission(levelIds, handicapIds, user.getId());
				// 同时查询用户的入款账号信息
				accountService.findIncomeAccountIdList4User(true, user.getId());
			}
			return mapper.writeValueAsString(
					new GeneralResponseData(ResponseStatus.SUCCESS.getValue(), user.getId().toString()));
		} catch (Exception e) {
			logger.error("更新失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
	}

	@RequestMapping("/alterPassword")
	public String alterPassword(@RequestParam(value = "id") Integer id,
			@RequestParam(value = "originalPassword") String originalPassword,
			@RequestParam(value = "currentPassword") String currentPassword) throws JsonProcessingException {
		try {
			logger.debug("修改密码，参数id{},originalPassword{},currentPassword{}", id, originalPassword, currentPassword);
			SysUser sysUser = userService.findFromCacheById(id);
			if (null == sysUser || null == sysUser.getPassword()) {
				return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "账号异常"));
			}
			if (!SaltPassword.checkPassword(originalPassword, sysUser.getPassword(), sysUser.getSalt())) {
				return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "原密码不正确"));
			}
			sysUser.setPassword(currentPassword);
			userService.saveOrUpdate(sysUser, 1, null, null, sysUser, null);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			logger.error("修改失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "操作失败"));
		}
	}

	@RequestMapping("/findByUserIdAndPropertyKey")
	public String findByUserIdAndPropertyKey(@RequestParam(value = "userId") Integer userId,
			@RequestParam(value = "propertyKey") String propertyKey) throws JsonProcessingException {
		try {
			GeneralResponseData<SysUserProfile> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = userService.findFromCacheById(userId);
			if (null == sysUser || null == sysUser.getPassword()) {
				return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "账号异常"));
			}
			responseData.setData(userProfileService.findByUserIdAndPropertyKey(userId, propertyKey));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("查询用户设置的区域分类失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "操作失败"));
		}
	}

	@RequestMapping("/saveByUserIdAndPropertyKey")
	public String saveByUserIdAndPropertyKey(@Param(value = "newProfile") SysUserProfile newProfile)
			throws JsonProcessingException {
		try {
			GeneralResponseData<SysUserProfile> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = userService.findFromCacheById(newProfile.getUserId());
			if (null == sysUser || null == sysUser.getPassword()) {
				return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "账号异常"));
			}
			SysUserProfile oldProfile = userProfileService.findByUserIdAndPropertyKey(newProfile.getUserId(),
					newProfile.getPropertyKey());
			if (null == oldProfile) {
				oldProfile = newProfile;
				// 防止接口传入ID修改到错误数据
				oldProfile.setId(null);
			} else {
				// 已存在配置，直接更新值
				oldProfile.setPropertyValue(newProfile.getPropertyValue());
			}
			userProfileService.saveAndFlush(oldProfile);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("保存用户设置的区域分类失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "操作失败"));
		}
	}

	private Map<String, Object> transUserToMap(SysUser user) {
		Map<String, Object> result = new HashMap<>();
		if (user == null) {
			return result;
		}
		result.put("id", user.getId());
		result.put("username", user.getUsername());
		result.put("phone", user.getPhone());
		result.put("email", user.getEmail());
		result.put("status", user.getStatus());
		result.put("createTime", user.getCreateTime());
		result.put("uid", user.getUid());
		result.put("category", user.getCategory());
		result.put("avatar", user.getAvatar());
		result.put("password", user.getPassword());
		int category = user.getCategory();
		result.put("category", category);
		if (category == com.xinbo.fundstransfer.domain.enums.UserCategory.Robot.getValue()) {
			result.put("categoryName", "机器");
		} else if (category <= UserCategory.Outward.getCode()) {
			result.put("categoryName", UserCategory.Outward.getName());
		} else if (UserCategory.isHandicapUser(category)) {
			BizHandicap handicap = handicapService.findFromCacheById(UserCategory.getHandicapId(category));
			result.put("categoryName", handicap == null ? StringUtils.EMPTY : handicap.getName());
		} else {
			UserCategory cat = UserCategory.findByCode(category);
			if (cat != null) {
				result.put("categoryName", cat.getName());
			}
		}
		result.put("handicap", user.getHandicap());
		return result;
	}

	@RequestMapping("/getPermissionKeyUser")
	public String getPermissionKeyUser(@Param(value = "permissionKey") String permissionKey) throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(operator)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			GeneralResponseData<List<SysUser>> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			List<SysUser> dataList = userService.findUserByPermissionKey(permissionKey);
			int admin = com.xinbo.fundstransfer.domain.enums.UserCategory.ADMIN.getValue();
			if (!CollectionUtils.isEmpty(dataList) && !Objects.equals(admin, operator.getCategory())) {
				int oprZone = handicapService.findZoneByHandiId(operator.getHandicap());
				Iterator<SysUser> iterator = dataList.iterator();
				while (iterator.hasNext()) {
					SysUser data = iterator.next();
					if (!Objects.equals(oprZone, handicapService.findZoneByHandiId(data.getHandicap())))
						iterator.remove();
				}
			}
			responseData.setData(dataList);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("根据permission_key查询用户失败:{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "根据permission_key查询用户失败"));
		}
	}
}
