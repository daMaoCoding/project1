package com.xinbo.fundstransfer.component.net.http.restTemplate;

import java.io.*;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;


import lombok.extern.slf4j.Slf4j;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.*;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;


/**
 * ************************
 *  Http 简单工具类
 * @author tony
 */
@Slf4j
public class RestTemplateUtil {

    private static PoolingHttpClientConnectionManager connMgr;
    private static RequestConfig requestConfig ;
    private static RestTemplate restTemplate ;
    private static HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = null;
    private static final HttpClientBuilder httpClientBuilder  =  HttpClientBuilder.create();
    private static final HttpClientBuilder httpClientBuilderRedirect = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig);
    private static final  HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
    private static final MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");


    static {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
            httpClientBuilder.setSSLContext(sslContext);
            httpClientBuilder.setRetryHandler(new RetryHandler());
            httpClientBuilderRedirect.setSSLContext(sslContext);
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();
            connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            connMgr.setMaxTotal(3000); // 将最大连接数
            connMgr.setDefaultMaxPerRoute(500);// 将每个路由基础的连接增加到20
            requestConfig = RequestConfig.custom()
                .setConnectTimeout(16 * 1000)    //  确定建立连接之前的超时时间,0无限制
                .setSocketTimeout(16 * 1000)    // 等待数据超时时间
                .setConnectionRequestTimeout(10*1000)//连接超时时间,连接不够用的等待时间，不宜过长，必须设置，比如连接不够用时
                .setMaxRedirects(200)
                .build();
            //HttpHost localhost = new HttpHost("www.hello.com", 80);
            //connMgr.setMaxPerRoute(new HttpRoute(localhost), 50);//将目标主机的最大连接数增加到50
            httpClientBuilder.setConnectionManager(connMgr);
            clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(getCloseableHttpClient());
            restTemplate = new RestTemplate(clientHttpRequestFactory);
            setMessageConverter(restTemplate);
        } catch (Exception e) {
            log.error("[RestTemplateUtil] Http连接池初始化错误，{}" , e.getMessage(), e);
        }
    }


    /**
     * 重试策略
     */
   static class RetryHandler implements HttpRequestRetryHandler {
        @Override
        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            if (executionCount <= 5){  //http故障，重试5次
                return true;
            }
            if (exception instanceof InterruptedIOException || exception instanceof NoHttpResponseException) {
                // Timeout or 服务端断开连接
                return true;
            }
            // Unknown host
            if (exception instanceof UnknownHostException) {
                return true;
            }
            // SSL handshake exception
            if (exception instanceof SSLException) {
                return true;
            }
            final HttpClientContext clientContext = HttpClientContext.adapt(context);
            final HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent) {
                // Retry if the request is considered idempotent
                return true;
            }
            return false;
        }
    }


    /**
     * 创建自动关闭httpClient
     */
    public static CloseableHttpClient getCloseableHttpClient(){
        CookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient client = httpClientBuilder.setKeepAliveStrategy(keepAliveStrat)
                                                      .setConnectionManagerShared(true)
                                                      .setConnectionManager(connMgr)
                                                      .setDefaultRequestConfig(requestConfig)
                                                      .setDefaultCookieStore(cookieStore)
                                                      .build();
        //CloseableHttpClient client = HttpClients.custom().setConnectionManager(connMgr).build();
        //return HttpClients.custom().setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).setDefaultCookieStore(cookieStore).build();
        return client;
    }


    /**
     * 设置 keepAlive
     */
    private static ConnectionKeepAliveStrategy keepAliveStrat = new DefaultConnectionKeepAliveStrategy() {
        @Override
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            long keepAlive = super.getKeepAliveDuration(response, context);
            if (keepAlive == -1) {
                //如果服务器没有设置keep-alive这个参数，我们就把它设置成60秒
                keepAlive = 60000;
            }
            return keepAlive;
        }

    };

    /**
     * 消息转换器
     */
    private static void setMessageConverter(RestTemplate restTemplate){
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        messageConverters.forEach(v->{
            if(v  instanceof StringHttpMessageConverter){
                ((StringHttpMessageConverter)v).setWriteAcceptCharset(false);
                ((StringHttpMessageConverter)v).setDefaultCharset(Charset.forName("UTF-8"));
            }
        });
        restTemplate.setMessageConverters(messageConverters);
    }



    /**
     * 创建header
     */
    public static HttpHeaders createNewHeader(Map<String,String> header){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        if(!CollectionUtils.isEmpty(header)){
            header.forEach((k,v)->{
                headers.add(k,v);
            });
        }
        return  headers;
    }





    /**
     * RestTemplate,工厂获取
     */
    public static  RestTemplate getRestTemplate(){
        return restTemplate;
    }



   /**
     * restFull 请求
     */
    public static  String postJson(String url, Object  obj,Map<String,String> header) {
        if (null != obj) {
            try {
                RestTemplate restTemplate = getRestTemplate();
                HttpHeaders headers = createNewHeader(header);
                HttpEntity<String> formEntity = null;
                if(obj instanceof  String && ((String) obj).contains("{") &&  ((String) obj).contains("}")){
                    formEntity =  new HttpEntity<String>((String)obj, headers);
                }else{
                   formEntity =  new HttpEntity<String>(JSON.toJSONString(obj), headers);
                }
                String result = restTemplate.postForObject(url, formEntity, String.class);
                return result;
            } catch (Exception ex) {
                log.error("发送http出错:{}", ex.getMessage(),ex);
            }
        }
        return "";
    }




}
