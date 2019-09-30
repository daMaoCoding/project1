package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.SysMenuPermission;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface SysMenuPermissionService {
	List<SysMenuPermission> getNodes(Integer parentId);

	List<Map<String, Object>> showAll(Integer roleId, HttpServletRequest request);

}
