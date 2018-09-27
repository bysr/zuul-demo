# Zuul说明 #
  
> Zuul是Netflix开源的微服务网关，基于JVM路由和服务端的负载均衡器。可以和Eureka、Ribbin、Hystrix等组件混合使用，Zuul核心是一系列过滤器。

> 在Spring Cloud体系中， Spring Cloud Zuul就是提供负载均衡、反向代理、权限认证的一个API gateway。

**==简单使用，分以下几步==**

##### 一. 负载均衡、路由转发功能

##### 二. 核心功能Filter，过滤器功能

##### 三. 路由重试和路由熔断功能

##### 四. 聚合微服务



## 负载均衡、路由转发功能
1. 添加依赖，创建Zuul服务 


```
        <!--注册到Eureka服务器-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <!--zuul相关组件-->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-zuul</artifactId>
        </dependency>
```
2. application.yml 添加服务设置以及eureka服务
        
        
        
```
server:
  port: 8040
spring:
  application:
    name: spring-cloud-zuul



eureka:
  client:
    fetch-registry: true
    register-with-eureka: true
    service-url:
      defaultZone: http://10.10.153.65:8000/eureka/

  instance:
    prefer-ip-address: true
    
# 除了spring-cloud-producer,其他的服务都忽略掉
zuul:
  ignored-services: '*'   #忽略所有请求
  routes:
    spring-cloud-producer: /producer/**    # 将服务名映射到producer，不设置访问地址为 http://localhost:8040/spring-cloud-producer/test/user/1
                                            # 映射后的访问地址：http://localhost:8040/producer/test/user/1
```
3. 启动类添加@EnableZuulProxy，用于支持网关路由
 
```
@SpringBootApplication
@EnableZuulProxy
public class SpringCloudZuulApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudZuulApplication.class, args);
        System.out.println("http://localhost:8040/producer/test/hello?name=neo&token=123");
    }

}

```

## 创建producer服务 ##
1. 添加依赖
```
    <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
```
2. 配置文件
```
spring:
  application:
    name: spring-cloud-producer
server:
  port: 9000
eureka:
  client:
    service-url:
      defaultZone: http://10.10.153.65:8000/eureka/
    enabled: true
  instance:
    prefer-ip-address: true


```
3. 控制器代码
```
@RestController
@RequestMapping(value = "/test")
public class TestController {
    private final Logger logger = LoggerFactory.getLogger(TestController.class);



    @RequestMapping("/hello")
    public String Hello(@RequestParam String name) {
        return "hello！" + name;
    }


}
```
4.启动类添加Eureka注解
```
@EnableDiscoveryClient
@SpringBootApplication
public class SpringCloudProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudProducerApplication.class, args);
    }
}
```
**分别启动producer和zuul服务**
- 访问producer服务

```
http://localhost:9000/test/hello?name=heo
结果：：hello！neo
```


- 访问zuul服务

```
http://localhost:8040/producer/test/hello?name=neo
结果：hello！neo
```
> 通过的zuul转发功能，实现了访问zuul服务----》 producer服务的转发功能

1. 模拟集群功能
  复制producer项目为producer2，修改Controller返回接口数据
```
@RestController
@RequestMapping(value = "/test")
public class TestController {


    @RequestMapping("/hello")
    public String Hello(@RequestParam String name) {
        return "hello！" + name + "-----2";
    }


}
```
2. 修改producer2的端口好为9001，修改完毕启动producer2

- 访问producer2服务
```
http://localhost:9001/test/hello?name=neo
结果：hello！neo-----2
```
- 访问zuul服务
```
http://localhost:8040/producer/test/hello?name=neo&token=123
交替显示内容
hello！neo
hello！neo-----2
hello！neo
...
```
**至此，路由转发和负载均衡的功能完成，聚合微服务的功能通过两个producer也能提现到**

## 过滤器功能 
- Filter是Zuul的核心，用来实现对外服务的控制。Filter的生命周期有4个，分别是“PRE”、“ROUTING”、“POST”、“ERROR”
1. 首先自定义一个Filter，这儿模拟的是token验证，在run()方法中验证参数是否含有Token
```
public class TokenFilter extends ZuulFilter {
    /**
     * pre：可以在请求被路由之前调用
     * route：在路由请求时候被调用
     * post：在route和error过滤器之后被调用
     * error：处理请求时发生错误时被调用
     *
     * @return
     */
    @Override
    public String filterType() {
        return "pre";//前置过滤器
    }

    @Override
    public int filterOrder() {
        return 0;  //优先级最高
    }

    @Override
    public boolean shouldFilter() {
        return true; //需要过滤
    }

    @Override
    public Object run() throws ZuulException {

        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        String token = request.getParameter("token");// 获取请求的参数
        if (StringUtils.isNotBlank(token)) {
            ctx.setSendZuulResponse(true); //认证通过，对请求进行路由
            ctx.setResponseStatusCode(200);
            ctx.set("isSuccess", true);
            return null;
        } else {
            ctx.setSendZuulResponse(false); //认证失败，不对其进行路由，返回错误消息
            ctx.setResponseStatusCode(400);
            ctx.setResponseBody("token is empty");
            ctx.set("isSuccess", false);
            return null;
        }
    }
}
```
2. 添加token验证到拦截队列中，在启动类中添加以下代码
```
@Bean
public TokenFilter tokenFilter() {
	return new TokenFilter();
}
```
- 访问如下地址：
```
http://localhost:8040/producer/test/hello?name=neo
结果：token is empty
```
> 结果被拦截

- 添加token后继续访问
```
http://localhost:8040/producer/test/hello?name=neo&token=123
结果：hello！neo
```
**过滤器功能测试通过！！！**
## 路由熔断
> 当我们的后端服务出现异常的时候，我们不希望将异常抛出给最外层，期望服务可以自动进行一降级。Zuul给我们提供了这样的支持。当某个服务出现异常时，直接返回我们预设的信息
- 通过自定义的fallback方法，并且将其指定给某个route来实现该route访问出问题的熔断处理。主要继承ZuulFallbackProvider接口来实现，ZuulFallbackProvider默认有两个方法，一个用来指明熔断拦截哪个服务，一个定制返回内容
```
@Component
public class ProducerFallback implements FallbackProvider {
    private final Logger logger = LoggerFactory.getLogger(FallbackProvider.class);

    /**
     * 指定要处理的service
     *
     * @return
     */
    @Override
    public String getRoute() {
        return "spring-cloud-producer";
    }

    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
        if (cause != null && cause.getCause() != null) {
            String reason = cause.getCause().getMessage();
            logger.info("Excption {}", reason);
        }
        return fallbackResponse();
    }

    public ClientHttpResponse fallbackResponse() {
        return new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() {
                return HttpStatus.OK;
            }

            @Override
            public int getRawStatusCode() {
                return 200;
            }

            @Override
            public String getStatusText() {
                return "OK";
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream("The service is unavailable.".getBytes());
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }

}

```
  - 当服务出现异常时，打印相关异常信息，并返回The service is unavailable. 关闭producer2，访问
```
http://localhost:8040/producer/test/hello?name=neo&token=123
结果：
hello！neo
The service is unavailable.
hello！neo
hello！neo
hello！neo
The service is unavailable.
...
```
- 多次访问之后,会一直出现
hello！ neo，也就是说,自动的寻找到正确响应的服务上去.错误的实例被抛弃.

>  根据结果看出来 spring-cloud-producer-2项目已经启用了熔断，返回:The service is unavailable.

> Zuul 目前只支持服务级别的熔断，不支持具体到某个URL进行熔断。

## 路由重试 
1. 添加依赖

```
     <!--开启重试功能-->
        <dependency>
            <groupId>org.springframework.retry</groupId>
            <artifactId>spring-retry</artifactId>
        </dependency>
```
2.添加配置
```

zuul:
  ignored-services: '*'
  routes:
    spring-cloud-producer: /producer/**


  retryable: true
ribbon:
  MaxAutoRetries: 3   //重试次数
  MaxAutoRetriesNextServer: 0
```
3. 再producer的控制器中添加接口
```
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
```
- 启动测试
```
http://localhost:8040/producer/test/go_hello?name=neo&token=123
结果：
The service is unavailable.

后台日志：
2018-09-27 14:20:38.468  INFO 7452 --- [nio-9000-exec-7] com.hipad.controller.TestController      : request two name is neo
2018-09-27 14:20:39.469  INFO 7452 --- [nio-9000-exec-8] com.hipad.controller.TestController      : request two name is neo
2018-09-27 14:20:40.469  INFO 7452 --- [io-9000-exec-10] com.hipad.controller.TestController      : request two name is neo
2018-09-27 14:20:41.470  INFO 7452 --- [nio-9000-exec-9] com.hipad.controller.TestController      : request two name is neo

```
-  设置的重试次数是3次，后台log打印了四次，符合预期


> 开启重试在某些情况下是有问题的，比如当压力过大，一个实例停止响应时，路由将流量转到另一个实例，很有可能导致最终所有的实例全被压垮。说到底，断路器的其中一个作用就是防止故障或者压力扩散。用了retry，断路器就只有在该服务的所有实例都无法运作的情况下才能起作用。这种时候，断路器的形式更像是提供一种友好的错误信息，或者假装服务正常运行的假象给使用者。

- 不用retry，仅使用负载均衡和熔断，就必须考虑到是否能够接受单个服务实例关闭和eureka刷新服务列表之间带来的短时间的熔断。如果可以接受，就无需使用retry。

## 聚合微服务
当前已经聚合了两个producer服务，添加一个文件上传服务
1. 创建microservice-file-upload项目，添加依赖
```
    <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!--thyleaf模版，首先Spring boot项目需要添加依赖，
        这样才能找到templates下面的*.html文件-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
```
2. 添加文件上传接口
```

    @RequestMapping(value = "/upload")
    @ResponseBody
    public String handleFileUpload(@RequestParam(value = "file", required = true) MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        File fileToSave = new File(file.getOriginalFilename());
        FileCopyUtils.copy(bytes, fileToSave);
        return fileToSave.getAbsolutePath();
    }
```
3. 配置文件
```
server:
  port: 8050
eureka:
  client:
    service-url:
      defaultZone: http://10.10.153.65:8000/eureka/
    enabled: true
  instance:
    prefer-ip-address: true
spring:
  application:
    name: microservice-file-upload

  http:
    multipart:
      max-file-size: 2000Mb
      max-request-size: 2500Mb
      location: /


info:
  app.name: microcloud-provider-upload
  company.name: com.alen
  build.artifactId: $project.artifactId$
  build.version: $project.verson$

```
4. 启动类中添加下面代码，用于支持大文件上传
```

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //允许上传的文件最大值
        factory.setMaxFileSize("2000MB"); //KB,MB
        /// 设置总上传数据总大小
        factory.setMaxRequestSize("2500MB");
        return factory.createMultipartConfig();
    }
```

5. zuul 服务中添加配置，转发文件上传
```
zuul:
  ignored-services: '*'
  routes:
    spring-cloud-producer: /producer/**
    microservice-file-upload: /file/**

  retryable: true
```



- 通过post访问上传文件接口
### 直接访问服务如下

```
http://localhost:8050/upload

返回成功
```

注意： 
- Headers中添加 Content-Type参数，值为multipart/form-data[    [测试有问题，去掉](https://blog.csdn.net/sanjay_f/article/details/47407063)]
- Body中添加文件file参数，选择file

### 访问zuul服务

```
http://localhost:8040/zuul/file/upload?token=123

返回成功
```
注意：
- zuul服务刚才添加过token拦截器，需要加上该参数
- zuul文件上传大文件，必须添加 /zuul 前缀，负责会报错
```
"Maximum upload size exceeded; nested exception is java.lang.IllegalStateException: org.apache.tomcat.util.http.fileupload.FileUploadBase$SizeLimitExceededException: the request was rejected because its size (14917605) exceeds the configured maximum (10485760)",
```
#### 至此  我们添加了三个微服务，两个请求服务，一个上传文件服务




代码中我通过html尝试上传，通过h5进行上传文件，页面会跳转到业务服务中，相当于zuul服务器没有使用，这个暂时没想好如何处理








备注：

ZUUL与NGINX其中一个区别是，只做服务接口的转发，不做页面的转发，这个需要在ZUUL做负载（同一个服务多个实例）时可以明显看的出来（如果只有一个实例，看不出来）

即，通过ZUUL访问服务的页面地址时，ZUUL访问路径会重定向跳转到实际的实例IP地址，而不是ZUUL服务的地址；如果你通过外网访问ZUUL网关地址并映到内网服务，最终浏览器上会跳转成内网的IP地址，无法访问，有时能打开了，但登录不了等等


