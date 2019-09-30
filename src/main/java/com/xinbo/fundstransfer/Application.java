package com.xinbo.fundstransfer;

import com.xinbo.fundstransfer.component.jpa.BaseRepositoryFactoryBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableRetry
@Slf4j
@EnableJpaRepositories(enableDefaultTransactions = false, basePackages = "com.xinbo.fundstransfer",repositoryFactoryBeanClass = BaseRepositoryFactoryBean.class)
@EnableScheduling
@Configuration
@EnableDiscoveryClient
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	public static void main(String[] args) {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		SpringApplication.run(Application.class, args);
	}


	@Bean
	public ErrorPageRegistrar errorPageRegistrar() {
		return registry -> {
			registry.addErrorPages(
					new ErrorPage(HttpStatus.UNAUTHORIZED, "/error401"),
					new ErrorPage(HttpStatus.NOT_FOUND, "/error404"),
					new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error500")
		);};
	}

}
