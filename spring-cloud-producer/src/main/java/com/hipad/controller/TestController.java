package com.hipad.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/test")
public class TestController {
    private final Logger logger = LoggerFactory.getLogger(TestController.class);


    @RequestMapping("/user/{name}")
    public String Index(@PathVariable(value = "name") String name) {
        return "hello！" + name;
    }

    @RequestMapping("/hello")
    public String Hello(@RequestParam String name) {
        return "hello！" + name;
    }

    @RequestMapping("/go_hello")
    public String goHello(@RequestParam String name) {
        logger.info("request two name is " + name);
        try {
            Thread.sleep(1000000);
        } catch (Exception e) {
            logger.error(" hello two error", e);
        }
        return "hello " + name + "，this is two messge";
    }


}
