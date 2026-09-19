package com.cet6.sprint.service;

import com.cet6.sprint.dto.LoginDTO;
import com.cet6.sprint.dto.RegisterDTO;
import com.cet6.sprint.entity.User;
import com.cet6.sprint.vo.LoginVO;

/**
 * 认证服务
 */
public interface AuthService {

    /** 注册 */
    void register(RegisterDTO dto);

    /** 登录，返回 token + 用户信息 */
    LoginVO login(LoginDTO dto);

    /** 当前登录用户 */
    User currentUser();

    /** 更新个人设置（考试日期、目标分、昵称） */
    User updateProfile(User form);
}
