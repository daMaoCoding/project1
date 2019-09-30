package com.xinbo.fundstransfer.newinaccount.controller;

import com.xinbo.fundstransfer.domain.GeneralResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author Administrator
 */
@RestControllerAdvice
@Slf4j
public class ValidParamExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseBody
	public GeneralResponseData handleMethodArgumentsValidException(MethodArgumentNotValidException ex) {
		log.error(ex.getMessage(), ex);
		BindingResult bindingResult = ex.getBindingResult();
		int size = bindingResult.getFieldErrors().size();
		StringBuilder errorMessage = new StringBuilder(size * 16);
		errorMessage.append("请求参数校验不通过:");
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				errorMessage.append(",");
			}
			FieldError fieldError = bindingResult.getFieldErrors().get(i);
			errorMessage.append(fieldError.getField());
			errorMessage.append(":");
			errorMessage.append(fieldError.getDefaultMessage());
		}
		GeneralResponseData responseData = new GeneralResponseData(-1, errorMessage.toString());
		return responseData;
	}
}
