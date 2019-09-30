package com.xinbo.fundstransfer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.SystemWebSocketCategory;
import com.xinbo.fundstransfer.domain.pojo.UserCategory;
import com.xinbo.fundstransfer.restful.BaseController;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.utils.HttpUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/auth")
public class AuthController extends BaseController {
    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);
    @Autowired
    private AppProperties appProperties;
    @Autowired
    AccountService accountService;
    @Autowired
    HandicapService handicapService;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login() {
        Subject currentUser = SecurityUtils.getSubject();
        if (currentUser.isAuthenticated()) {
            return MessageFormat.format("redirect:{0}/{1}/html/index.html", HttpUtils.getRealDomain(),appProperties.getVersion());
        }
        return MessageFormat.format("redirect:{0}/{1}/html/login.html", HttpUtils.getRealDomain(),appProperties.getVersion());
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(@RequestParam(FormAuthenticationFilter.DEFAULT_USERNAME_PARAM) String username,
                        Map<String, Object> map, ServletRequest request) {

        String msg = parseException(request);

        map.put("msg", msg);
        map.put("username", username);

        return MessageFormat.format("forward:/{0}/html/login.html", appProperties.getVersion());
    }

    @RequestMapping(value = "/login", method = {RequestMethod.POST}, headers = "x-requested-with=XMLHttpRequest")
    public @ResponseBody
    String login(HttpServletRequest request) throws JsonProcessingException {
        if (SecurityUtils.getSubject().isAuthenticated()) {
            GeneralResponseData json = new GeneralResponseData(ResponseStatus.SUCCESS.getValue(),
                    getMessage("response.success"));
            json.setData(getLoginInfo());
            return mapper.writeValueAsString(json);
        } else {
            return mapper.writeValueAsString(
                    new SimpleResponseData(ResponseStatus.FAIL.getValue(), parseException(request)));
        }
    }

    //web登陆成功
    @RequestMapping(value = "/login/success", method = {RequestMethod.GET})
    public @ResponseBody
    String success(javax.servlet.http.HttpServletRequest request, HttpServletResponse response)
            throws JsonProcessingException {
        GeneralResponseData json = new GeneralResponseData(ResponseStatus.SUCCESS.getValue(),
                getMessage("response.success"));
        json.setData(getLoginInfo());
        SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
        SimpleCookie cookieUserId = new SimpleCookie("JUSERID");
        cookieUserId.setValue(user.getId().toString());
        cookieUserId.setHttpOnly(false);
        cookieUserId.saveTo(request, response);

        SimpleCookie cookieUid = new SimpleCookie("JUID");
        cookieUid.setValue(user.getUid());
        cookieUid.setHttpOnly(false);
        cookieUid.saveTo(request, response);

        SimpleCookie cookieUserName = new SimpleCookie("JUSERNAME");
        try {
            String userName = URLEncoder.encode(user.getUsername(), "UTF-8");
            cookieUserName.setValue(userName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        cookieUserName.setHttpOnly(false);
        cookieUserName.saveTo(request, response);

        SimpleCookie cookieUserCategory = new SimpleCookie("JUSERCATEGORY");
        int category = user.getCategory() < UserCategory.Outward.getCode()
                && user.getCategory() != com.xinbo.fundstransfer.domain.enums.UserCategory.ADMIN.getValue()
                && user.getCategory() != com.xinbo.fundstransfer.domain.enums.UserCategory.Robot.getValue()
                ? UserCategory.Outward.getCode() : user.getCategory();
        cookieUserCategory.setValue(String.valueOf(category));
        cookieUserCategory.setHttpOnly(false);
        cookieUserCategory.saveTo(request, response);

        SimpleCookie cookieUserZone = new SimpleCookie("JUSERZONE");
        int zone = 0;
        if (Objects.nonNull(user.getHandicap()) && !Objects.equals(user.getHandicap(), 0) && !Objects
                .equals(user.getCategory(), com.xinbo.fundstransfer.domain.enums.UserCategory.ADMIN.getValue()))
            zone = handicapService.findZoneByHandiId(user.getHandicap());
        cookieUserZone.setValue(String.valueOf(zone));
        cookieUserZone.setHttpOnly(false);
        cookieUserZone.saveTo(request, response);
        return mapper.writeValueAsString(json);
    }

    @RequestMapping("/logout")
    public String logout(HttpServletRequest request, ServletResponse response) throws IOException {
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.logout();
        } catch (Exception e) {
            LOG.error("Logout error.", e);
        }
        return MessageFormat.format("redirect:{0}/{1}/html/login.html", HttpUtils.getRealDomain(), appProperties.getVersion());
    }

    @RequestMapping(value = "/login", method = {RequestMethod.GET}, params = "restful=true")
    public @ResponseBody
    String timeout() throws JsonProcessingException {
        return loginDialog();
    }

    @RequestMapping(method = {RequestMethod.GET}, headers = "X-Requested-With=XMLHttpRequest")
    public @ResponseBody
    String loginDialog() throws JsonProcessingException {
        SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
        if (null != loginUser) {
            String info = CommonUtils.genSysMsg4WS(loginUser.getId(), SystemWebSocketCategory.System,
                    "unauthentication");
            redisService.convertAndSend(RedisTopics.BROADCAST, info);
        }
        return mapper.writeValueAsString(
                new SimpleResponseData(ResponseStatus.FAIL.getValue(), getMessage("response.error") + ", 会话超时，请重新登录。"));
    }

    @RequestMapping(value = "/unauthorized", method = {RequestMethod.GET, RequestMethod.POST})
    public @ResponseBody
    String unauthorized() throws JsonProcessingException {
        return loginDialog();
    }

    private Map<String, Object> getLoginInfo() {
        Map<String, Object> dataToMap = new HashMap<>();
        SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
        dataToMap.put("user", sysUser);
        return dataToMap;
    }

    private String parseException(ServletRequest request) {
        String errorString = (String) request.getAttribute(FormAuthenticationFilter.DEFAULT_ERROR_KEY_ATTRIBUTE_NAME);
        Class<?> error = null;
        try {
            if (errorString != null) {
                error = Class.forName(errorString);
            }
        } catch (ClassNotFoundException e) {
            LOG.error("", e);
        }

        String msg = "其他错误！";
        if (error != null) {
            if (error.equals(UnknownAccountException.class)) {
                msg = "未知帐号错误！";
            } else if (error.equals(IncorrectCredentialsException.class)) {
                msg = "密码错误！";
            } else if (error.equals(AuthenticationException.class)) {
                msg = "认证失败！";
            } else if (error.equals(DisabledAccountException.class)) {
                msg = "账号被冻结！";
            }
        }

        return "登录失败，" + msg;
    }

}
