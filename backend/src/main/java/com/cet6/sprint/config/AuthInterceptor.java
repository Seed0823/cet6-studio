package com.cet6.sprint.config;

import com.cet6.sprint.common.JwtUtil;
import com.cet6.sprint.common.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cet6.sprint.common.Result;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * <p>
 * 校验请求头 Authorization: Bearer &lt;token&gt;，
 * 通过则把 userId 塞进 UserContext，失败直接返回 401 JSON。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 放行预检请求，否则跨域会失败
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        try {
            String token = auth.substring(7);
            Claims claims = jwtUtil.parse(token);
            UserContext.setUserId(Long.valueOf(claims.getSubject()));
            return true;
        } catch (Exception e) {
            log.debug("token 校验失败: {}", e.getMessage());
            writeUnauthorized(response, "登录状态已失效，请重新登录");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 必须清理，避免线程复用导致用户串号
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        // 语义化返回 HTTP 401：网关 / 监控 / 浏览器 DevTools 都能直接识别「未认证」，
        // 而不是一个「200 但 body 里写 401」的伪成功响应。body 仍是统一 Result 结构。
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, message)));
    }
}
