package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.dto.LoginDTO;
import com.cet6.sprint.dto.RegisterDTO;
import com.cet6.sprint.entity.User;
import com.cet6.sprint.service.AuthService;
import com.cet6.sprint.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 注册 */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.ok("注册成功", null);
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok("登录成功", authService.login(dto));
    }

    /** 当前用户信息 */
    @GetMapping("/me")
    public Result<User> me() {
        return Result.ok(authService.currentUser());
    }

    /** 修改个人设置 */
    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody User form) {
        return Result.ok("已保存", authService.updateProfile(form));
    }
}
