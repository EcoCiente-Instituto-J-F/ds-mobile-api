package br.com.ecociente.mobile.config.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.ecociente.mobile.core.domain.autenticacao.PerfilUsuarioType;
import br.com.ecociente.mobile.core.domain.autenticacao.Usuario;
import br.com.ecociente.mobile.core.gateway.autenticacao.TokenProvaiderPort;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService implements TokenProvaiderPort {

    private final SecretKey secretKey;
    private final JwtParser parser;
    private final long expirationMs;

    @Autowired
    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms:300000}") long expirationMs) {
        if (expirationMs < 1000
                || expirationMs / 1000 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Expiração do JWT fora do intervalo permitido");
        }

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));

        this.parser = Jwts.parser()
                .verifyWith(secretKey)
                .build();

        this.expirationMs = expirationMs;
    }

    public JwtService(String secret) {
        this(secret, 300000L);
    }

    @Override
    public String gerarToken(
            Usuario usuario,
            PerfilUsuarioType perfil) {
        Instant agora = Instant.now();

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claims(
                        Map.of(
                                "usuarioId", usuario.getId(),
                                "nome", usuario.getNome(),
                                "perfil", perfil.name()))
                .issuedAt(Date.from(agora))
                .expiration(
                        Date.from(
                                agora.plusMillis(expirationMs)))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public Long getExpiracaoSegundos() {
        return expirationMs / 1000;
    }

    public Claims validarToken(String token) {
        Claims claims = parser
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getExpiration() == null
                || !claims.getExpiration().after(new Date())) {
            throw new JwtException(
                    "Token sem expiração válida");
        }

        Integer usuarioId = claims.get(
                "usuarioId",
                Integer.class);

        if (usuarioId == null || usuarioId <= 0) {
            throw new JwtException(
                    "Token sem identificação válida");
        }

        if (claims.getSubject() == null
                || claims.getSubject().isBlank()) {
            throw new JwtException(
                    "Token sem e-mail");
        }

        return claims;
    }
}