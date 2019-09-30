package com.xinbo.fundstransfer.restful;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import com.xinbo.fundstransfer.CommonUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysRole;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.entity.SysUserRole;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.SystemWebSocketCategory;
import com.xinbo.fundstransfer.domain.pojo.UserCategory;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.MenuInitialService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SysRoleService;
import com.xinbo.fundstransfer.service.SysUserProfileService;
import com.xinbo.fundstransfer.service.SysUserRoleService;
import com.xinbo.fundstransfer.service.SysUserService;

@RestController
@RequestMapping("/r/role")
public class SysRoleController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(SysRoleController.class);

	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private SysRoleService sysRoleService;
	@Autowired
	private SysUserRoleService sysUserRoleService;
	@Autowired
	private MenuInitialService menuInitialService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysUserProfileService sysUserProfileService;

	@RequestMapping("/list")
	public String getList(@RequestParam(value = "pageNo", defaultValue = "0", required = false) int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "level");
			Page<SysRole> page = sysRoleService.findAll(pageRequest);
			GeneralResponseData<List<SysRole>> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			List<SysRole> roleList = (List<SysRole>) page.getContent();
			// 获取每个角色下面有多少人.
			List<Object> rolePeoples = sysRoleService.countPeopleByRoId();
			for (int i = 0; i < roleList.size(); i++) {
				SysRole role = roleList.get(i);
				for (int j = 0; j < rolePeoples.size(); j++) {
					Object[] obj = (Object[]) rolePeoples.get(j);
					if (role.getId() == obj[0]) {
						role.setCounts(Integer.valueOf(obj[1].toString()));
					}
				}
			}
			responseData.setData(null != roleList && roleList.size() > 0 ? roleList : null);
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("获取角色失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(ResponseStatus.FAIL.getValue(), "查询失败  "));
		}
	}

	@RequestMapping("/findByUserId")
	public String findByUserId(@RequestParam(value = "userId") Integer userId) throws JsonProcessingException {
		try {
			logger.debug("通过用户id获取角色参数：userId{}", userId);
			List<Map<String, Object>> result = new ArrayList<>();
			Set<Integer> selRoleSet = new HashSet<>();
			List<SysUserRole> userRole = sysUserRoleService.findByUserId(userId);
			Specification<SysRole> specif = DynamicSpecifications.build(SysRole.class,
					userId == 88888888 ? null : new SearchFilter("visible", SearchFilter.Operator.EQ, 1));
			List<SysRole> role = sysRoleService.findList(specif);
			Collections.sort(role, new Comparator<SysRole>() {
				public int compare(SysRole p1, SysRole p2) {
					// 按照SysRole的等级进行升序排列
					if ((p1.getLevel() == null ? 0 : p1.getLevel()) > (p2.getLevel() == null ? 0 : p2.getLevel())) {
						return -1;
					}
					if ((p1.getLevel() == null ? 0 : p1.getLevel()) == (p2.getLevel() == null ? 0 : p2.getLevel())) {
						return 0;
					}
					return 1;
				}
			});
			// 获取当前用户角色
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<SysUserRole> userRoleList = sysUserRoleService.findByUserId(loginUser.getId());
			List<Integer> ids = new ArrayList<>();
			for (int i = 0; i < userRoleList.size(); i++) {
				SysUserRole sysUserRole = userRoleList.get(i);
				ids.add(sysUserRole.getRoleId());
			}
			List<SysRole> roleList = sysRoleService.findAllByIdIn(ids);
			// 一个用户可能存在多个角色，按照最高等级来查询下面的角色
			Collections.sort(roleList, new Comparator<SysRole>() {
				public int compare(SysRole p1, SysRole p2) {
					// 按照SysRole的等级进行升序排列
					if ((p1.getLevel() == null ? 0 : p1.getLevel()) > (p2.getLevel() == null ? 0 : p2.getLevel())) {
						return 1;
					}
					if ((p1.getLevel() == null ? 0 : p1.getLevel()) == (p2.getLevel() == null ? 0 : p2.getLevel())) {
						return 0;
					}
					return -1;
				}
			});
			int level = roleList.size() == 0 ? 0
					: (roleList.get(roleList.size() - 1).getLevel() == null ? 0
							: roleList.get(roleList.size() - 1).getLevel());
			if (userId != null) {
				for (int i = 0; i < userRole.size(); i++) {
					SysUserRole sysUserRole = userRole.get(i);
					selRoleSet.add(sysUserRole.getRoleId());
					// 如果本身有这个角色，又被禁用了，也要显示到已分配的角色里面去。
					boolean flg = true;
					for (int j = 0; j < role.size(); j++) {
						SysRole sysRole = role.get(j);
						if (sysRole.getId().equals(sysUserRole.getRoleId())) {
							flg = false;
							break;
						}
					}
					if (flg) {
						role.add(sysRoleService.findById(sysUserRole.getRoleId()));
					}
				}
			}
			role.forEach((p) -> {
				Map<String, Object> item = new HashMap<>();
				// 系统管理员不做限制。
				if (loginUser.getCategory() == -1) {
					item.put("id", p.getId());
					item.put("name", p.getName());
					item.put("selected", selRoleSet.contains(p.getId()));
					result.add(item);
				} else {
					// 已经分配到的角色不做等级限制
					for (int j = 0; j < userRole.size(); j++) {
						if (p.getId().equals(userRole.get(j).getRoleId())
								&& level < (p.getLevel() == null ? 0 : p.getLevel())) {
							item.put("id", p.getId());
							item.put("name", p.getName());
							item.put("selected", selRoleSet.contains(p.getId()));
							result.add(item);
						}
					}
					// 角色下拉框不受等级限制.
					if (level >= (p.getLevel() == null ? 0 : p.getLevel()) || userId == 88888888) {
						item.put("id", p.getId());
						item.put("name", p.getName());
						item.put("selected", selRoleSet.contains(p.getId()));
						result.add(item);
					}
				}
			});
			GeneralResponseData<List<?>> responseData = new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue());
			responseData.setData(result);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("通过用户id获取角色失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<List>(ResponseStatus.FAIL.getValue(), "初始化角色失败"));
		}
	}

	@RequestMapping("/get")
	public String findById(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		try {
			logger.debug("通过id获取角色参数：id{}", id);
			SysRole sysRole = sysRoleService.findById(id);
			GeneralResponseData<SysRole> responseData = new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue());
			responseData.setData(sysRole);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("通过id获取角色失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(ResponseStatus.FAIL.getValue(), "查询记录失败"));
		}
	}

	@RequestMapping("/save")
	public String save(@Valid SysRole roleVo) throws JsonProcessingException {
		try {
			logger.debug("保存角色参数：roleVo{}", roleVo);
			// 新增默认可见
			roleVo.setVisible(1);
			roleVo = sysRoleService.saveOrUpdate(roleVo);
			GeneralResponseData<SysRole> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "保存角色成功。");
			responseData.setData(roleVo);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("保存角色参数失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败:" + e.getMessage()));
		}
	}

	@RequestMapping("/alertRoleOfUser")
	public String alertRoleOfUser(@RequestParam(value = "userId") Integer userId,
			@RequestParam(value = "roleIdArray") Integer[] roleIdArray) throws JsonProcessingException {
		try {
			logger.debug("修改角色参数：userId{}", userId);
			sysUserRoleService.alertRoleOfUser(userId, roleIdArray);
			// 对受影响的用户，推送菜单信息到前端
			List<Map<String, Object>> menuList = menuInitialService.findMenuList(userId);
			String msg = mapper.writeValueAsString(menuList);
			String info = CommonUtils.genSysMsg4WS(userId, SystemWebSocketCategory.MenuList, msg);
			redisService.convertAndSend(RedisTopics.BROADCAST, info);
			redisService.convertAndSend(RedisTopics.REFRESH_MENUPERMISSION, String.valueOf(userId));
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(), "保存成功"));
		} catch (Exception e) {
			logger.error("修改角色失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "保存失败"));
		}
	}

	@RequestMapping("/delete")
	public String delete(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		try {
			logger.debug("删除角色参数：id{}", id);
			sysRoleService.delete(id);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			logger.error("删除角色失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "操作失败"));
		}
	}

	@RequestMapping("/deleteUser")
	public String deleteUser(@RequestParam(value = "userId") Integer userId) throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			SysUser user = sysUserService.findFromCacheById(userId);
			logger.info("deleteUser删除用户参数:userId:{},id:{},username:{},uid:{},handicap:{}", operator.getUid(),
					user.getId(), user.getUsername(), user.getUid(), user.getHandicap());
			// 如果用户手上有出款卡不允许删除
			if (sysUserService.findHolders(userId) > 0) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "用户手上有在用的出款卡请回收后在删除!"));
			}
			// 判断用户是否在接单,如果在则不允许删除
			if (!CollectionUtils.isEmpty(
					redisService.getStringRedisTemplate().keys("IncomeAuditAccountAllocate:" + userId + ":" + "*"))) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "用户正在审核入款单不能删除!"));
			}
			// 删除之前退出账号登录
			String closeMsg = CommonUtils.genCloseMsg4WS(userId, null, null, AppConstants.LOGOUT_WS, null);
			SpringContextUtils.getBean(RedisService.class).convertAndSend(RedisTopics.CLOSE_WEBSOCKET, closeMsg);
			sysUserService.deleteUser(userId);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			logger.error("删除角色失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "操作失败"));
		}
	}

	@RequestMapping("/findPerplesByRoleId")
	public String findPerplesByRoleId(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "roleId") Integer roleId, @RequestParam(value = "type") Integer type)
			throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "create_time");
			account = !"".equals(account) ? account : null;
			Map<String, Object> mapp = sysUserService.findPerplesByRoleId(type, account, roleId, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> userList = (List<Object>) page.getContent();
			List<SysUser> arrlist = new ArrayList<SysUser>();
			for (int i = 0; i < userList.size(); i++) {
				Object[] obj = (Object[]) userList.get(i);
				SysUser user = new SysUser();
				user.setId(Integer.valueOf(obj[0].toString()));
				user.setUsername((String) obj[1]);
				user.setUid((String) obj[8]);
				// 如果大于400则取减去400的盘口当着分类
				UserCategory userCategory = UserCategory.findByCode((int) obj[9]);
				if (Integer.valueOf(obj[9].toString()) > 400) {
					BizHandicap bizHandicap = handicapService
							.findFromCacheById((Integer.valueOf(obj[9].toString()) - 400 * 1));
					user.setClassification(bizHandicap.getName());
				} else {
					user.setClassification(userCategory == null ? "" : userCategory.getName());
				}
				SysUserProfile outLimit = sysUserProfileService.findByUserIdAndPropertyKey(
						Integer.valueOf(obj[0].toString()), UserProfileKey.OUTDRAW_MONEYLIMIT.getValue());
				SysUserProfile audiLimit = sysUserProfileService.findByUserIdAndPropertyKey(
						Integer.valueOf(obj[0].toString()), UserProfileKey.INCOME_AUDITLIMIT.getValue());
				user.setOutLimit(outLimit == null ? "0" : outLimit.getPropertyValue());
				user.setAuditLimit(audiLimit == null ? "0" : audiLimit.getPropertyValue());
				user.setStatus(Integer.valueOf(obj[5].toString()));
				user.setCreateTimeStr((String) obj[12]);
				arrlist.add(user);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("查询角色下面所属人员失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "操作失败"));
		}
	}

	@RequestMapping("/undo")
	public String cancelRole(@RequestParam(value = "userIds", required = false) String[] userIds,
			@RequestParam(value = "roleId") Integer roleId) throws JsonProcessingException {
		try {
			List<String> userIdsList = Arrays.asList(userIds);
			sysUserService.deleteUserRole(roleId, userIdsList);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			logger.error("撤销角色失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "操作失败"));
		}
	}

	@RequestMapping("/addUserToRole")
	public String addUserToRole(@RequestParam(value = "userIds", required = false) String[] userIds,
			@RequestParam(value = "roleId") Integer roleId) throws JsonProcessingException {
		try {
			List<String> userIdsList = Arrays.asList(userIds);
			sysUserService.addUserToRole(roleId, userIdsList);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			logger.error("添加用户到角色失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "操作失败"));
		}
	}

}
