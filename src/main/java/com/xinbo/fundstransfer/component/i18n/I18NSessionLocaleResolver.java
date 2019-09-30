package com.xinbo.fundstransfer.component.i18n;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

@Component("localeResolver")
public class I18NSessionLocaleResolver extends SessionLocaleResolver {
	private Locale locale;

	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		return locale == null ? request.getLocale() : locale;
	}

	@Override
	public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		this.locale = locale;
	}
}
