package com;



import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CharacterEncodingFilter;

@SpringBootApplication
public class SpringBootApiApplication {
	public static void main(String[] args) {
				
		SpringApplication.run(SpringBootApiApplication.class, args);
	}
	
		@Bean
	public FilterRegistrationBean<CharacterEncodingFilter> filterRegistrationBean() {
	    FilterRegistrationBean<CharacterEncodingFilter> registrationBean = new FilterRegistrationBean<>();
	    registrationBean.setFilter(new CharacterEncodingFilter());
	    registrationBean.setOrder(Integer.MIN_VALUE);
	    registrationBean.setAsyncSupported(true);  // Enable async support
	    return registrationBean;
	}

}
