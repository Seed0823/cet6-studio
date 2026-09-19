package com.cet6.sprint.common;

/**
 * 当前登录用户上下文
 * <p>
 * 拦截器校验 JWT 通过后，把 userId 放进 ThreadLocal；
 * 业务层通过 {@code UserContext.getUserId()} 直接取，不用在每个方法上传参。
 * 请求结束后务必 {@code remove()}，否则线程池复用会造成数据串号 / 内存泄漏。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        Long id = USER_ID.get();
        if (id == null) {
            throw new BusinessException(401, "未登录或登录已失效");
        }
        return id;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
