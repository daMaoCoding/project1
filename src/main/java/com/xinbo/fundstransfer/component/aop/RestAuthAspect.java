package com.xinbo.fundstransfer.component.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.service.RedisService;

//@Aspect
//@Component
public class RestAuthAspect {

	@Autowired
	private RedisService redisService;

	private ObjectMapper mapper = new ObjectMapper();

//	@Pointcut("execution(* com.xinbo.fundstransfer.restful.api..*.*(..))")
	public void doAuthentication() {

	}

//	@Around("doAuthentication()")
	public Object doAuthentication(ProceedingJoinPoint point) throws Throwable {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

		String uid = attributes.getRequest().getParameter("uid");
		String token = attributes.getRequest().getParameter("token");

		String methodName = point.getSignature().getName();
		Class<?> classTarget = point.getTarget().getClass();
		Class<?>[] par = ((MethodSignature) point.getSignature()).getParameterTypes();
		Method objMethod = classTarget.getMethod(methodName, par);

		Annotation[] o = objMethod.getAnnotations();
		if (o != null) {
			System.out.println("-----------------");
			System.out.println("-----------------获取注解实现类上的注解");
			System.out.println("-----------------");
		}

		// if (StringUtils.isEmpty(uid) || StringUtils.isEmpty(token)) {
		// return mapper.writeValueAsString(new
		// SimpleResponseData(ResponseStatus.FAIL.getValue(), "参数值为空"));
		// }
		//
//		return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(), "访问被拒绝，未授权"));
//		 if (!redisService.checkToken(uid, token)) {
//			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(), "访问被拒绝，未授权"));
//		}
//
		return point.proceed();
	}

}