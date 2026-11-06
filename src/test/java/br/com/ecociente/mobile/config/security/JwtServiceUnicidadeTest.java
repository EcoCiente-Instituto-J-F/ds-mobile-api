package br.com.ecociente.mobile.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import br.com.ecociente.mobile.core.domain.autenticacao.PerfilUsuarioType;
import br.com.ecociente.mobile.core.domain.autenticacao.Usuario;
import io.jsonwebtoken.Claims;

class JwtServiceUnicidadeTest {

    private static final String CHAVE = "ecociente-chave-testes-jwt-minimo-32-bytes-12345";

    @Test
    void tokensConsecutivosDoMesmoUsuarioTemJtiDiferente() {
        JwtService servico = new JwtService(CHAVE);
        Usuario usuario = Usuario.builder()
                .id(42)
                .nome("Usuario Teste")
                .email("teste@invalid.example")
                .build();

        String primeiro = servico.gerarToken(usuario, PerfilUsuarioType.USUARIO_COMUM);
        String segundo = servico.gerarToken(usuario, PerfilUsuarioType.USUARIO_COMUM);

        assertNotEquals(primeiro, segundo);
        Claims claimsPrimeiro = servico.validarToken(primeiro);
        Claims claimsSegundo = servico.validarToken(segundo);
        assertNotNull(claimsPrimeiro.getId());
        assertNotNull(claimsSegundo.getId());
        assertNotEquals(claimsPrimeiro.getId(), claimsSegundo.getId());
        assertEquals(42, claimsPrimeiro.get("usuarioId", Integer.class));
        assertEquals(42, claimsSegundo.get("usuarioId", Integer.class));
    }

    @Test
    void variasEmissoesNoMesmoSegundoNaoColidem() {
        JwtService servico = new JwtService(CHAVE);
        Usuario usuario = Usuario.builder()
                .id(42)
                .nome("Usuario Teste")
                .email("teste@invalid.example")
                .build();

        Set<String> tokens = new HashSet<>();
        Set<String> identificadores = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            String token = servico.gerarToken(usuario, PerfilUsuarioType.USUARIO_COMUM);
            Claims claims = servico.validarToken(token);
            assertTrue(tokens.add(token));
            assertNotNull(claims.getId());
            assertFalse(claims.getId().isBlank());
            assertTrue(identificadores.add(claims.getId()));
        }

        assertEquals(50, tokens.size());
        assertEquals(50, identificadores.size());
    }
}
