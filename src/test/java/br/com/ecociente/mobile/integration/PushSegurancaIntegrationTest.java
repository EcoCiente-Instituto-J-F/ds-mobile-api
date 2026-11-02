package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import br.com.ecociente.mobile.config.notificacoes.PushAudience;
import br.com.ecociente.mobile.config.security.JwtService;
import br.com.ecociente.mobile.config.security.SecurityConfig;
import br.com.ecociente.mobile.dataprovider.gateway.notificacoes.JdbcPushService;
import br.com.ecociente.mobile.entrypoint.controller.notificacoes.PushController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Tag("integracao")
@Testcontainers
@SpringBootTest(classes = PushSegurancaIntegrationTest.Configuracao.class, properties = {
        "app.security.jwt.secret=chave-notificacoes-integracao-0123456789abcdef",
        "app.push.enabled=true",
        "app.push.audience=all",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false"
})
@AutoConfigureMockMvc
@Sql("/sql/notificacoes-seguranca-test.sql")
class PushSegurancaIntegrationTest {
    private static final String INSTALACAO_1 = "00000000-0000-4000-8000-000000000001";
    private static final String INSTALACAO_2 = "00000000-0000-4000-8000-000000000002";
    private static final String PREFERENCIAS = """
            {"notificacoesGerais":true,"lembretesAvisos":false,
             "novasAtividades":true,"dicasSustentaveis":false}
            """;
    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            "chave-notificacoes-integracao-0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.com.ecociente.mobile.config.security.UsuarioDetailsService usuarioDetailsServiceTeste;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcPushService push;

    private String token(int usuarioId) {
        return Jwts.builder().subject("usuario@teste.invalid")
                .claim("usuarioId", usuarioId).claim("perfil", "MORADOR")
                .expiration(Date.from(Instant.now().plusSeconds(600))).signWith(KEY).compact();
    }

    private String corpo(String instalacao, String token, String preferencias) {
        return "{\"instalacaoId\":\"" + instalacao + "\",\"token\":\"" + token
                + "\",\"preferencias\":" + preferencias + "}";
    }

    @Test
    void exigeJwtParaRegistrarConsultarEMarcarLida() throws Exception {
        mvc.perform(post("/api/notificacoes/dispositivos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(INSTALACAO_1, "token-10", PREFERENCIAS)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notificacoes")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/notificacoes/1/lida")).andExpect(status().isUnauthorized());
    }

    @Test
    void recusaPreferenciasIncompletasSemRegistrarToken() throws Exception {
        mvc.perform(post("/api/notificacoes/dispositivos")
                .header("Authorization", "Bearer " + token(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(INSTALACAO_1, "token-10", "{}")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/notificacoes/dispositivos")
                .header("Authorization", "Bearer " + token(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(INSTALACAO_1, "token-10",
                        "{\"notificacoesGerais\":null,\"lembretesAvisos\":true,\"novasAtividades\":true,\"dicasSustentaveis\":true}")))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM tb_push_dispositivos", Integer.class));
    }

    @Test
    void dispositivoPertenceAoJwtENaoAoConteudoDoPedido() throws Exception {
        mvc.perform(post("/api/notificacoes/dispositivos")
                .header("Authorization", "Bearer " + token(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(INSTALACAO_1, "token-10", PREFERENCIAS)))
                .andExpect(status().isNoContent());
        assertEquals(10, jdbc.queryForObject(
                "SELECT usuario_id FROM tb_push_dispositivos WHERE instalacao_id = ?",
                Integer.class, INSTALACAO_1));
        assertEquals(false, jdbc.queryForObject(
                "SELECT preferencias ->> 'lembretesAvisos' FROM tb_push_dispositivos WHERE instalacao_id = ?",
                String.class, INSTALACAO_1).equals("true"));
    }

    @Test
    void usuarioNaoPodeDesativarDispositivoDeOutraConta() throws Exception {
        mvc.perform(post("/api/notificacoes/dispositivos")
                .header("Authorization", "Bearer " + token(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(INSTALACAO_1, "token-10", PREFERENCIAS)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/notificacoes/dispositivos/" + INSTALACAO_1)
                .header("Authorization", "Bearer " + token(20)))
                .andExpect(status().isNoContent());
        assertTrue(jdbc.queryForObject(
                "SELECT ativo FROM tb_push_dispositivos WHERE instalacao_id = ?",
                Boolean.class, INSTALACAO_1));
        mvc.perform(delete("/api/notificacoes/dispositivos/" + INSTALACAO_1)
                .header("Authorization", "Bearer " + token(10)))
                .andExpect(status().isNoContent());
        assertFalse(jdbc.queryForObject(
                "SELECT ativo FROM tb_push_dispositivos WHERE instalacao_id = ?",
                Boolean.class, INSTALACAO_1));
    }

    @Test
    void rotacaoDeTokenDesativaInstalacaoAnterior() throws Exception {
        mvc.perform(post("/api/notificacoes/dispositivos")
                .header("Authorization", "Bearer " + token(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(INSTALACAO_1, "token-compartilhado", PREFERENCIAS)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/notificacoes/dispositivos")
                .header("Authorization", "Bearer " + token(20))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(INSTALACAO_2, "token-compartilhado", PREFERENCIAS)))
                .andExpect(status().isNoContent());
        assertFalse(jdbc.queryForObject(
                "SELECT ativo FROM tb_push_dispositivos WHERE instalacao_id = ?",
                Boolean.class, INSTALACAO_1));
        assertTrue(jdbc.queryForObject(
                "SELECT ativo FROM tb_push_dispositivos WHERE instalacao_id = ?",
                Boolean.class, INSTALACAO_2));
    }

    @Test
    void historicoELeituraSaoIsoladosPeloJwt() throws Exception {
        assertTrue(push.criar(10, "quiz", "quiz:10", "Quiz 10", "Mensagem 10", "quiz", null));
        assertTrue(push.criar(20, "quiz", "quiz:20", "Quiz 20", "Mensagem 20", "quiz", null));
        int daConta10 = jdbc.queryForObject(
                "SELECT id_notificacao FROM tb_notificacoes WHERE usuario_id = 10", Integer.class);

        mvc.perform(get("/api/notificacoes")
                .header("Authorization", "Bearer " + token(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titulo").value("Quiz 10"));

        mvc.perform(patch("/api/notificacoes/" + daConta10 + "/lida")
                .header("Authorization", "Bearer " + token(20)))
                .andExpect(status().isNoContent());
        assertNull(jdbc.queryForObject(
                "SELECT lida_em FROM tb_notificacoes WHERE id_notificacao = ?",
                java.sql.Timestamp.class, daConta10));
        mvc.perform(patch("/api/notificacoes/" + daConta10 + "/lida")
                .header("Authorization", "Bearer " + token(10)))
                .andExpect(status().isNoContent());
        assertNotNull(jdbc.queryForObject(
                "SELECT lida_em FROM tb_notificacoes WHERE id_notificacao = ?",
                java.sql.Timestamp.class, daConta10));
    }

    @Test
    void chaveEventoImpedeDuplicidadePorUsuario() {
        assertTrue(push.criar(10, "quiz", "tentativa:1", "Quiz", "Corpo", "quiz", null));
        assertFalse(push.criar(10, "quiz", "tentativa:1", "Quiz", "Corpo", "quiz", null));
        assertTrue(push.criar(20, "quiz", "tentativa:1", "Quiz", "Corpo", "quiz", null));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM tb_notificacoes", Integer.class));
    }

    @Test
    void resultadoDeColetaNaoVaiParaUsuarioQueNaoEhSindico() {
        assertFalse(push.criar(20, "solicitacao_aceita", "coleta:1", "Coleta", "Corpo", "calendario", null));
        assertTrue(push.criar(10, "solicitacao_aceita", "coleta:1", "Coleta", "Corpo", "calendario", null));
        assertEquals(10, jdbc.queryForObject("SELECT usuario_id FROM tb_notificacoes", Integer.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class
    })
    @Import({SecurityConfig.class, JwtService.class, PushController.class, JdbcPushService.class, PushAudience.class})
    static class Configuracao {}
}
