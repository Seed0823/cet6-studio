package com.cet6.sprint.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 工具类
 * <p>
 * 负责签发和校验 token。token 里只放 userId 和 username，不放敏感信息
 * （JWT 的 payload 是 Base64 编码，任何人都能解开看）。
 */
@Component
public class JwtUtil {

    private final Key key;
    private final long expireMillis;

    public JwtUtil(@Value("${cet6.jwt.secret}") String secret,
                   @Value("${cet6.jwt.expire-hours}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 60 * 60 * 1000;
    }

    /** 签发 token */
    public String createToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析 token，非法或过期会抛异常 */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** 从 token 中取 userId */
    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }
}
