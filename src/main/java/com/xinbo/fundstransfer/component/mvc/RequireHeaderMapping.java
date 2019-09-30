package com.xinbo.fundstransfer.component.mvc;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.utils.IpUtils;
import com.xinbo.fundstransfer.utils.SimpleResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.sound.midi.Soundbank;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ************************
 * 请求Header(FROM_KEY) 及IP 过滤限制
 * @author tony
 */

@Slf4j
@Configuration
public class RequireHeaderMapping  implements BeanPostProcessor {

    @Autowired  private AppProperties appProperties;
//  @Autowired  private RequestMappingHandlerMapping requestMappingHandlerMapping;
    private  static final String errorHeaderAndIp =  JSON.toJSONString(SimpleResponseData.error("请求IP受限..."));
    private  static final String errorHeader =  JSON.toJSONString(SimpleResponseData.error("请求Header ["+HeaderCondition.requireHeaderKey+"] 错误。"));
    private  static Map<String, String> headerConditionMap = Maps.newTreeMap();


    /**
     * 检查ip是否白名单
     * [如果有其他系统需要过滤，同样配置]
     */
    private boolean checkIp(String reqFrom, String ip){
        if(StringUtils.isBlank(reqFrom))
            return false;
        reqFrom = reqFrom.toUpperCase();
        switch (reqFrom){
            case HeaderCondition.ptCrkHeader: //来自平台的请求头
                return CollectionUtils.isEmpty(appProperties.getPT_CRK_IPS()) || appProperties.getPT_CRK_IPS().contains(ip) ;
            case HeaderCondition.chatPayHeader: //来自聊天室请求头
                return CollectionUtils.isEmpty(appProperties.getCHAT_PAY_IPS()) || appProperties.getCHAT_PAY_IPS().contains(ip) ;
            //here-->增加其他需验证的系统header
            default:
                return true;
        }
    }




    @Bean
    public FilterRegistrationBean signValidateFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        HeaderFilter headerFilter = new HeaderFilter(this);
        registration.setFilter(headerFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MAX_VALUE);
        return registration;
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }



    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        try {
            //只处理Controller
           if( AnnotationUtils.isAnnotationDeclaredLocally(Controller.class, bean.getClass()) ||AnnotationUtils.isAnnotationDeclaredLocally(RestController.class, bean.getClass())   ){
               //类含有header限制，类中的mappting方法自动增加header限制
               if(isClassWithRequireHeader(bean)) {
                   //类的requireHeader值
                   String classRequireHeader =findClassRequireHeader(bean);
                   if (StringUtils.isNotBlank(classRequireHeader)) {
                       //类的baseUrl
                       Set<String> classBaseUrlsSet =  findClassBaseUrls(bean);
                       Method[] allDeclaredMethods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());
                       for (int i = 0; i < allDeclaredMethods.length; i++) {
                           Set<String> methodUrls = findMethodUrls(allDeclaredMethods[i]);
                           if(!CollectionUtils.isEmpty(classBaseUrlsSet)){
                               classBaseUrlsSet.forEach(x->{methodUrls.forEach(y->{ headerConditionMap.put(x+y,classRequireHeader);});});
                           }else{
                               methodUrls.forEach(y->{ headerConditionMap.put(y,classRequireHeader); });
                           }
                       }
                   }

               }else {
                   //不支持基于方法的注解
               }
            }
        }catch (Exception e){
            log.error("创建[RequireHeaderMapping]出错,{},程序退出。",e.getMessage(),e);
            System.exit(1);
        }
        return bean;
    }




    /**
     * 过滤请求header-ip 白名单
     * 1.验证请求controler是否需要携带指定Header
     * 2.验证指定header是否配置为白名单。
     */
    public boolean checkRequireHeader(ServletRequest request, ServletResponse response){
        if(CollectionUtils.isEmpty(headerConditionMap)) return true;
        try {
            HttpServletRequest req = (HttpServletRequest)request;
            String reqFrom =  req.getHeader(HeaderCondition.requireHeaderKey); //固定请求头
            String reqIp = IpUtils.getIp(req);      //请求ip
            String reqUri = req.getRequestURI();
            if(StringUtils.isBlank(reqUri) ||StringUtils.isBlank(reqIp) ||"localhost".equalsIgnoreCase(reqIp) || "127.0.0.1".equalsIgnoreCase(reqIp)) return true;
            if(needCheckHeader(reqUri)){
                if(!checkHeaderCondition(reqFrom,reqUri)){
                    log.error("请求Header错误,Header(FROM_KEY):{}，IP:{},URI:{}",reqFrom,reqIp,req.getRequestURL());
                    respHeaderError(response,errorHeader);
                    return false;
                }
                if(!checkIp(reqFrom, reqIp)){
                    log.error("请求IP受限,Header(FROM_KEY):{}，IP:{},URI:{}",reqFrom,reqIp,req.getRequestURL());
                    respHeaderError(response,errorHeaderAndIp);
                    return false;
                }
            }
            return true;
        }catch (Exception e){
            log.error("过滤器检查Header/ip是否白名单异常：{}",e.getMessage(),e);
            return true;
        }
    }


    /**
     * 检查接口调用者传递的Header是否是接口规定的值
     *
     */
    private boolean checkHeaderCondition(String reqFrom,String reqUri){
        String conditionHeader = headerConditionMap.get(reqUri);
        if(conditionHeader!=null&& conditionHeader.equalsIgnoreCase(reqFrom)){
            return true;
        }
        return false;
    }


    /**
     * 检查是否需要验证Header
     */
    private boolean needCheckHeader(String reqUri){
        String conditionHeader = headerConditionMap.get(reqUri);
        if(conditionHeader==null || StringUtils.isBlank(conditionHeader)) return false;
        return true;
    }


    /**
     * 响应错误
     */
    private void respHeaderError(ServletResponse response,String msg) throws IOException {
        PrintWriter writer = null;
        OutputStreamWriter osw = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        try {
            osw = new OutputStreamWriter(response.getOutputStream() , "UTF-8");
            writer = new PrintWriter(osw, true);
            writer.write(msg);
            writer.flush();
            writer.close();
            osw.close();
           } finally {
            if (null != writer) {
                writer.close();
            }
            if(null != osw){
                osw.close();
            }
        }
    }



    /**
     * 查找类的RequireHeader注解内容
     */
    private String findClassRequireHeader(Object bean){
        return StringUtils.trimToNull( AnnotationUtils.findAnnotation(bean.getClass(), RequireHeader.class).value());
    }


    /**
     * 含有 RequireHeader 注释的类
     */
    private boolean isClassWithRequireHeader(Object bean){
        return AnnotationUtils.isAnnotationDeclaredLocally(RequireHeader.class, bean.getClass());
    }


    /**
     * 查找类的 request Mapping，过滤含有 @PathVariable 的url
     */
    private Set<String> findClassBaseUrls(Object bean){
        Set<String> classBaseUrlsSet = Sets.newHashSet();
        if( AnnotationUtils.isAnnotationDeclaredLocally(RequestMapping.class, bean.getClass()) ){
            String [] classBaseUrls =   AnnotationUtils.findAnnotation(bean.getClass(), RequestMapping.class).value();
            if(null!=classBaseUrls && classBaseUrls.length>0) classBaseUrlsSet = Stream.of(classBaseUrls ).filter(x -> !x.contains("{") || !x.contains("}")).collect(Collectors.toSet());
        }
        return classBaseUrlsSet;
    }


    /**
     * 查找方法的  request Mapping，过滤含有 @PathVariable 的url
     */
    private Set<String> findMethodUrls(Method method){
        Set<String> methodUrlsSet = Sets.newHashSet();
        if(method.isAnnotationPresent(RequestMapping.class)){
            String  [] methodUrls =method.getAnnotation(RequestMapping.class).value();
            if(null!=methodUrls && methodUrls.length>0) methodUrlsSet = Stream.of(methodUrls ).filter(x -> !x.contains("{") || !x.contains("}")).collect(Collectors.toSet());
        }
        return methodUrlsSet;
    }



}
