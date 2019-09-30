package com.xinbo.fundstransfer.component.mvc;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import java.io.IOException;
@Slf4j
public class HeaderFilter implements Filter{

    RequireHeaderMapping requireHeaderMapping;

    public HeaderFilter(RequireHeaderMapping requireHeaderMapping) {
        this.requireHeaderMapping = requireHeaderMapping;
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(null!=requireHeaderMapping && requireHeaderMapping.checkRequireHeader(request,response)){
            chain.doFilter(request, response);
        }
       // chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }





}
