package com.cet6.sprint.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 登录结果
 */
@Data
public class LoginVO {

    private String token;

    private Long userId;

    private String username;

    private String nickname;

    private LocalDate examDate;

    private Integer targetScore;
}
