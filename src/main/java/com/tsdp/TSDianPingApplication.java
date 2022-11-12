package com.tsdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author 范大晨
 * @since 2022-11-9
 */
@MapperScan("com.tsdp.mapper")
@SpringBootApplication
public class TSDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TSDianPingApplication.class, args);
    }

}
