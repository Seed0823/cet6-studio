package com.cet6.sprint.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cet6.sprint.common.BusinessException;
import com.cet6.sprint.common.JwtUtil;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.dto.LoginDTO;
import com.cet6.sprint.dto.RegisterDTO;
import com.cet6.sprint.entity.User;
import com.cet6.sprint.mapper.UserMapper;
import com.cet6.sprint.service.AuthService;
import com.cet6.sprint.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该账号已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        // 绝不明文存密码：BCrypt 自带随机盐，同一密码每次哈希结果都不同
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank()
                ? dto.getUsername() : dto.getNickname());
        // 默认目标：2026 年 12 月 19 日（12 月第二个周六）
        user.setExamDate(LocalDate.of(2026, 12, 19));
        user.setTargetScore(425);
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, dto.getUsername()));

        // 账号不存在和密码错误返回同一句提示，避免被枚举出有效账号
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
        }

        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.createToken(user.getId(), user.getUsername()));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setExamDate(user.getExamDate());
        vo.setTargetScore(user.getTargetScore());
        return vo;
    }

    @Override
    public User currentUser() {
        return userMapper.selectById(UserContext.getUserId());
    }

    @Override
    public User updateProfile(User form) {
        Long userId = UserContext.getUserId();
        User db = userMapper.selectById(userId);
        if (db == null) {
            throw new BusinessException("用户不存在");
        }
        if (form.getNickname() != null && !form.getNickname().isBlank()) {
            db.setNickname(form.getNickname());
        }
        if (form.getExamDate() != null) {
            db.setExamDate(form.getExamDate());
        }
        if (form.getTargetScore() != null) {
            db.setTargetScore(form.getTargetScore());
        }
        // password 不在此处修改，避免被覆盖成 null
        userMapper.updateById(db);
        db.setPassword(null);
        return db;
    }
}
