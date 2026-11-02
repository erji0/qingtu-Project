package com.qingtu.security;

import com.qingtu.entity.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-hours:24}")
    private long expireHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(SysUser user) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("name", user.getName())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expire)
                .signWith(key())
                .compact();
    }

    public LoginUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Object rawId = claims.get("userId");
        Long userId = rawId == null ? null : Long.valueOf(String.valueOf(rawId));
        return new LoginUser(userId,
                claims.getSubject(),
                claims.get("name", String.class),
                claims.get("role", String.class));
    }
}
