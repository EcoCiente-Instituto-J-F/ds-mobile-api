package br.com.ecociente.mobile.config.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final JwtParser parser;

    public JwtService(
        @Value("${app.security.jwt.secret}") String secret
    ) {
        this.parser = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
            ))
            .build();
    }

    public Claims validarToken(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();

        if (claims.getExpiration() == null
            || !claims.getExpiration().after(new Date())) {
            throw new JwtException("Token sem expiração válida");
        }

        Integer usuarioId = claims.get("usuarioId", Integer.class);

        if (usuarioId == null || usuarioId <= 0) {
            throw new JwtException("Token sem identificação válida");
        }

        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new JwtException("Token sem e-mail");
        }

        return claims;
    }
}