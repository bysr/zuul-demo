package com.hipad;

import com.hipad.filter.TokenFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableZuulProxy
public class SpringCloudZuulApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudZuulApplication.class, args);
        System.out.println("http://localhost:8040/producer/test/hello?name=neo&token=123");
    }


    @Bean
    public TokenFilter tokenFilter() {
        return new TokenFilter();
    }
}
