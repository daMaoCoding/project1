package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.entity.SysMenuPermission;
import com.xinbo.fundstransfer.domain.entity.SysRoleMenuPermission;
import com.xinbo.fundstransfer.domain.repository.SysMenuPermissionRepository;
import com.xinbo.fundstransfer.service.SysMenuPermissionService;
import com.xinbo.fundstransfer.service.SysRoleMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
@Slf4j
public class SysMenuPermissionServiceImpl implements SysMenuPermissionService {

	@Autowired
	private SysMenuPermissionRepository sysMenuPermissionRepository;
	@Autowired
	private SysRoleMenuService sysRoleMenuService;

	@Override
	public List<SysMenuPermission> getNodes(Integer parentId) {
		Sort sort = new Sort(Sort.Direction.ASC, "id");
		return sysMenuPermissionRepository.findByParentId(parentId, sort);
	}

	@Override
	public List<Map<String, Object>> showAll(Integer roleId, HttpServletRequest request) {
		List<Map<String, Object>> respList = new ArrayList<>();
		try {
			SysMenuPermission sysMenu = new SysMenuPermission();
			sysMenu.setParentId(null);
			respList = treeMenuList(sysMenu.getParentId(), roleId);
			respList = removeDuplicate(respList);

		} catch (Exception e) {
			log.error("service查询失败" + e.getStackTrace());
		}
		return ((respList != null) && (respList.size() > 0)) ? respList : null;
	}

	public List<Map<String, Object>> treeMenuList(Integer parentId, Integer roleId) {
		List<Map<String, Object>> list = new LinkedList<>();
		List<SysMenuPermission> parentList = getNodes(parentId);
		if (parentList != null && parentList.size() > 0) {
			List<Map<String, Object>> childMpList = new LinkedList<>();
			Iterator<SysMenuPermission> iterator = parentList.iterator();
			while (iterator.hasNext()) {
				SysMenuPermission sysMenuPermission = iterator.next();
				Map<String, Object> childMp = new LinkedHashMap<>();
				childMp.put("id", sysMenuPermission.getId());
				childMp.put("name", sysMenuPermission.getName().replace("&nbsp;", ""));
				childMp.put("pId", sysMenuPermission.getParentId());
				if (sysMenuPermission.getParentId() == null) {
					childMp.put("isParent", true);
				}
				SysRoleMenuPermission sysRoleMenuPermission = sysRoleMenuService.getByRoleIdAndMenuPermissionId(roleId,
						sysMenuPermission.getId());

				if (null != sysRoleMenuPermission && null != sysRoleMenuPermission.getMenuPermissionId()) {
					childMp.put("checked", true);
					childMp.put("open", true);
				}

				childMpList.add(childMp);
				list.addAll(childMpList);
				List<Map<String, Object>> nextChildMpList = treeMenuList(sysMenuPermission.getId(), roleId);
				list.addAll(nextChildMpList);
			}
		}
		return list;
	}

	public static List removeDuplicate(List list) {
		HashSet h = new LinkedHashSet(list);
		list.clear();
		list.addAll(h);
		return list;
	}
}
