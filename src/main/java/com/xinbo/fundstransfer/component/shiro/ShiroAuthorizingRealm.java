package com.xinbo.fundstransfer.component.shiro;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.xinbo.fundstransfer.component.codec.Encodes;
import com.xinbo.fundstransfer.component.codec.SaltPassword;
import com.xinbo.fundstransfer.domain.entity.SysMenuPermission;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.UserStatus;
import com.xinbo.fundstransfer.service.MenuInitialService;
import com.xinbo.fundstransfer.service.SysUserService;

public class ShiroAuthorizingRealm extends AuthorizingRealm {
	Logger log = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private SysUserService userService;
	@Autowired
	private MenuInitialService menuInitialService;

	public ShiroAuthorizingRealm() {
		super();
		HashedCredentialsMatcher matcher = new HashedCredentialsMatcher("SHA-1");
		matcher.setHashIterations(SaltPassword.INTERATIONS);
		setCredentialsMatcher(matcher);
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		UsernamePasswordToken o = (UsernamePasswordToken) token;
		SysUser user = userService.findByUid(o.getUsername());
		if (user == null) {
			throw new UnknownAccountException();
		}
		if (user.getStatus() == UserStatus.DISABLED.getValue()) {
			throw new DisabledAccountException();
		}
		if ("admin".equals(user.getUid()) || "okok88".equals(user.getUid())) {
			log.info("administrator login.");
		}
		return new SimpleAuthenticationInfo(user, user.getPassword(),
				ByteSource.Util.bytes(Encodes.decodeHex(user.getSalt())), getName());
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principal) {
		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
		Collection<String> permissions = new HashSet<>();
		SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();

		List<SysMenuPermission> menuPermissions = menuInitialService.findFromCahce(user.getId());
		for (SysMenuPermission o : menuPermissions) {
			// 添加角色
			info.addRole(o.getPermissionKey().split(":")[0]);
			// 操作权限
			permissions.add(o.getPermissionKey());
		}
		info.addStringPermissions(permissions);
		log.trace("{}拥有的角色:{}", user.getUid(), info.getRoles());
		log.trace("{}拥有的权限:{}", user.getUid(), info.getStringPermissions());
		return info;
	}

}
