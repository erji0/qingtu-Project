package com.qingtu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.qingtu.mapper")
@SpringBootApplication
public class QingtuApplication {

    public static void main(String[] args) {
        SpringApplication.run(QingtuApplication.class, args);
    }
}
