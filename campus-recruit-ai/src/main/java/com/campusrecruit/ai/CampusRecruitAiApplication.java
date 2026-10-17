package com.campusrecruit.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campusrecruit.ai.mapper")
public class CampusRecruitAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusRecruitAiApplication.class, args);
    }
}
