package com.xinbo.fundstransfer.restful;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.xinbo.fundstransfer.CommonUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.entity.SysRoleMenuPermission;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserRole;
import com.xinbo.fundstransfer.domain.pojo.SystemWebSocketCategory;
import com.xinbo.fundstransfer.service.MenuInitialService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SysMenuPermissionService;
import com.xinbo.fundstransfer.service.SysRoleMenuService;
import com.xinbo.fundstransfer.service.SysUserRoleService;
import com.xinbo.fundstransfer.service.SysUserService;

@RestController
@RequestMapping(value = "/r/menu")
public class SysRoleMenuPermissionController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(SysRoleMenuPermissionController.class);
	@Autowired
	HttpServletRequest request;
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private SysMenuPermissionService sysMenuPermissionService;
	@Autowired
	private SysRoleMenuService sysRoleMenuService;
	@Autowired
	private SysUserService userService;
	@Autowired
	private MenuInitialService menuInitialService;
	@Autowired
	private SysUserRoleService userRoleService;
	@Autowired
	private RedisService redisService;

	@RequestMapping("/get")
	public String initialMenu(@RequestParam(value = "uid") String uid) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		logger.debug("通过用户id获取菜单：uid:{}", uid);
		SysUser user = userService.findByUid(uid);
		SysUser currentUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			List<Map<String, Object>> list = (user != null && user.getId() != null)
					? menuInitialService.findMenuList(user.getId())
					: (currentUser != null && currentUser.getId() != null)
							? menuInitialService.findMenuList(currentUser.getId()) : Collections.emptyList();
			responseData.setData(list);
		} catch (Exception e) {
			logger.error("通过用户id获取菜单失败：异常:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	@RequestMapping(value = "/initial")
	public String initial(@RequestParam(value = "roleId") Integer roleId) throws JsonProcessingException {
		logger.debug("进入初始化菜单权限调用:{}", roleId);
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			List<Map<String, Object>> list = sysMenuPermissionService.showAll(roleId, request);
			responseData.setData(list);
		} catch (Exception e) {
			logger.error("初始化菜单权限调用失败:{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(ResponseStatus.FAIL.getValue(), "操作失败"));
		}
		return mapper.writeValueAsString(responseData);

	}

	@RequestMapping("/save")
	public String saveByRoleId(@RequestParam(value = "roleId") Integer roleId,
			@RequestParam(value = "menuIdArr") Integer[] menuIdArr) throws JsonProcessingException {
		logger.debug("开始保存菜单权限,参数:roleId{},menuIdArr{}", roleId, menuIdArr);
		GeneralResponseData<Integer> responseData = new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(),
				"保存成功");
		try {
			List<SysRoleMenuPermission> list = sysRoleMenuService.saveByRoleId(roleId, menuIdArr);
			if (list != null && list.size() > 0) {
				responseData.setData(list.size());
			}
			// 对受影响的用户，推送菜单信息到前端
			new Thread(() -> {
				List<SysUserRole> userRoleList = userRoleService.findByRoleId(roleId);
				for (SysUserRole userRolel : userRoleList) {
					try {
						redisService.convertAndSend(RedisTopics.REFRESH_MENUPERMISSION,
								String.valueOf(userRolel.getUserId()));
						List<Map<String, Object>> menuList = menuInitialService.findMenuList(userRolel.getUserId());
						String msg = mapper.writeValueAsString(menuList);
						String info = CommonUtils.genSysMsg4WS(userRolel.getUserId(), SystemWebSocketCategory.MenuList,
								msg);
						redisService.convertAndSend(RedisTopics.BROADCAST, info);
					} catch (Exception e) {
						logger.error("" + e);
					}
				}
			}).start();

		} catch (Exception e) {
			logger.error("保存菜单权限失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(ResponseStatus.FAIL.getValue(), "保存失败"));
		}
		return mapper.writeValueAsString(responseData);
	}

}
