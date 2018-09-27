package com.hipad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;

import javax.servlet.MultipartConfigElement;

@SpringBootApplication
@EnableEurekaClient
public class MicroserviceFileUploadApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroserviceFileUploadApplication.class, args);

        System.out.println("http://localhost:8050");
    }


    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //允许上传的文件最大值
        factory.setMaxFileSize("2000MB"); //KB,MB
        /// 设置总上传数据总大小
        factory.setMaxRequestSize("2500MB");
        return factory.createMultipartConfig();
    }


}
