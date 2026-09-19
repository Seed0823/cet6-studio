package com.cet6.sprint;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CET-6 六级冲刺学习平台 · 启动类
 *
 * @author 崔嘉兵
 */
@SpringBootApplication
@MapperScan("com.cet6.sprint.mapper")
public class Cet6SprintApplication {

    public static void main(String[] args) {
        SpringApplication.run(Cet6SprintApplication.class, args);
        System.out.println("""
                ================================================
                  CET-6 Sprint 后端启动成功
                  接口地址: http://localhost:8080
                ================================================
                """);
    }
}
