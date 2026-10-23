package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.Date;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.ecociente.mobile.config.security.JwtService;
import br.com.ecociente.mobile.config.security.SecurityConfig;
import br.com.ecociente.mobile.core.domain.NovaAvaliacaoColeta;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException;
import br.com.ecociente.mobile.core.gateway.AvaliacaoColetaGateway;
import br.com.ecociente.mobile.core.service.AvaliarColetaService;
import br.com.ecociente.mobile.dataprovider.gateway.AvaliacaoColetaGatewayImpl;
import br.com.ecociente.mobile.entrypoint.controller.AvaliacaoColetaController;
import br.com.ecociente.mobile.entrypoint.exception.AvaliacaoColetaExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.AvaliacaoColetaMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Testcontainers
@SpringJUnitConfig(AvaliarColetaIntegrationTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
@Execution(ExecutionMode.SAME_THREAD)
class AvaliarColetaIntegrationTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  WebApplicationContext context;

  @Autowired
  DataSource dataSource;

  @Autowired
  JdbcTemplate jdbc;

  @Autowired
  AvaliacaoColetaGateway gateway;

  private MockMvc mvc;

  @BeforeEach
  void preparar() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      statement.execute("DROP SCHEMA public CASCADE");
      statement.execute("CREATE SCHEMA public");

      statement.execute(recurso(
          "sql/ecociente-schema.sql"));

      statement.execute(recurso(
          "sql/avaliar-coleta-dados-test.sql"));
    }

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Test
  void sindicoDeveAvaliarColetaRealizadaSemVisitaRegistrada()
      throws Exception {

    mvc.perform(post("/api/agendamentos/100/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":5,\"comentario\":\"  Equipe pontual  \"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.agendamentoId").value(100))
        .andExpect(jsonPath("$.visitaId").exists())
        .andExpect(jsonPath("$.nota").value(5))
        .andExpect(jsonPath("$.comentario").value("Equipe pontual"))
        .andExpect(jsonPath("$.avaliadoEm").exists());

    // a visita nasce das datas do agendamento e já fica confirmada
    assertEquals(1L, jdbc.queryForObject("""
        SELECT COUNT(*) FROM tb_visitas_coletas
        WHERE agendamento_coleta_id = 100
          AND foi_realizada = TRUE AND houve_confirmacao = TRUE
        """, Long.class));

    assertEquals(5, jdbc.queryForObject("""
        SELECT a.nota
        FROM tb_avaliacoes_visitas_coletas a
        JOIN tb_visitas_coletas v ON v.id_visita_coleta = a.visita_coleta_id
        WHERE v.agendamento_coleta_id = 100 AND a.usuario_avaliador_id = 30
        """, Integer.class));
  }

  @Test
  void deveUsarAVisitaRealizadaQueJaExiste() throws Exception {
    mvc.perform(post("/api/agendamentos/104/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":3}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.visitaId").value(1004))
        .andExpect(jsonPath("$.comentario").doesNotExist());

    assertEquals(1L, jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_visitas_coletas WHERE agendamento_coleta_id = 104",
        Long.class));
  }

  @Test
  void notaDeveEntrarNaMediaDaCooperativa() throws Exception {
    mvc.perform(post("/api/agendamentos/100/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":2}"))
        .andExpect(status().isCreated());

    // já havia uma nota 4 na coleta 101
    assertEquals(0, new java.math.BigDecimal("3.00").compareTo(
        jdbc.queryForObject("""
            SELECT ROUND(AVG(a.nota), 2)
            FROM tb_avaliacoes_visitas_coletas a
            JOIN tb_visitas_coletas v ON v.id_visita_coleta = a.visita_coleta_id
            JOIN tb_agendamentos_coletas ag
              ON ag.id_agendamento_coleta = v.agendamento_coleta_id
            WHERE ag.cooperativa_id = 1
            """, java.math.BigDecimal.class)));
  }

  @Test
  void naoDeveAvaliarDuasVezes() throws Exception {
    mvc.perform(post("/api/agendamentos/101/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":5}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("AVALIACAO_JA_REGISTRADA"));

    assertEquals(1L, jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_avaliacoes_visitas_coletas WHERE visita_coleta_id = 1001",
        Long.class));
  }

  @Test
  void segundaAvaliacaoDoMesmoAgendamentoDeveSerRecusada()
      throws Exception {

    String corpo = "{\"nota\":4}";

    mvc.perform(post("/api/agendamentos/100/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo))
        .andExpect(status().isCreated());

    mvc.perform(post("/api/agendamentos/100/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("AVALIACAO_JA_REGISTRADA"));
  }

  @Test
  void naoDeveAvaliarColetaQueNaoFoiRealizada() throws Exception {
    mvc.perform(post("/api/agendamentos/102/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":5}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("COLETA_NAO_REALIZADA"));

    assertEquals(0L, jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_visitas_coletas WHERE agendamento_coleta_id = 102",
        Long.class));
  }

  @ParameterizedTest
  @ValueSource(ints = { 10, 31 })
  void soOSindicoDoCondominioPodeAvaliar(int usuario) throws Exception {
    mvc.perform(post("/api/agendamentos/100/avaliacao")
        .header("Authorization", bearer(usuario))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":5}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("AVALIACAO_NAO_PERMITIDA"));

    assertEquals(0L, jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_visitas_coletas WHERE agendamento_coleta_id = 100",
        Long.class));
  }

  @Test
  void deveRetornar404ParaAgendamentoInexistente() throws Exception {
    mvc.perform(post("/api/agendamentos/999/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":5}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("AGENDAMENTO_NAO_ENCONTRADO"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "6", "-1" })
  void deveRejeitarNotaForaDoIntervalo(String nota) throws Exception {
    mvc.perform(post("/api/agendamentos/100/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":" + nota + "}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ERRO_VALIDACAO"));
  }

  @Test
  void deveRejeitarCorpoSemNota() throws Exception {
    mvc.perform(post("/api/agendamentos/100/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"comentario\":\"ok\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ERRO_VALIDACAO"));
  }

  @Test
  void deveRejeitarComentarioLongoDemais() throws Exception {
    mvc.perform(post("/api/agendamentos/100/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":5,\"comentario\":\"" + "a".repeat(501) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ERRO_VALIDACAO"));
  }

  @Test
  void deveRejeitarJsonInvalidoEIdInvalido() throws Exception {
    mvc.perform(post("/api/agendamentos/100/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("isto nao e json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));

    mvc.perform(post("/api/agendamentos/abc/avaliacao")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":5}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
  }

  @Test
  void gatewayDeveTraduzirAUnicidadeDoBancoEmAvaliacaoJaRegistrada() {
    // a visita 1001 já tem avaliação: a restrição única do banco precisa virar erro de domínio
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> gateway.registrar(
            1001,
            NovaAvaliacaoColeta.de(30, 101, 5, null)));

    assertEquals("AVALIACAO_JA_REGISTRADA", erro.getCodigo());
  }

  @Test
  void deveExigirJwt() throws Exception {
    mvc.perform(post("/api/agendamentos/100/avaliacao")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nota\":5}"))
        .andExpect(status().isUnauthorized());
  }

  private String bearer(int usuario) {
    String perfil = usuario == 10
        ? "COOPERATIVA"
        : "SINDICO";

    String token = Jwts.builder()
        .subject("usuario-" + usuario + "@teste.com")
        .claim("usuarioId", usuario)
        .claim("perfil", perfil)
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(
            Instant.now().plusSeconds(600)))
        .signWith(Keys.hmacShaKeyFor(
            SECRET.getBytes(StandardCharsets.UTF_8)))
        .compact();

    return "Bearer " + token;
  }

  private String recurso(String caminho) throws Exception {
    try (var stream = new ClassPathResource(caminho)
        .getInputStream()) {

      return new String(
          stream.readAllBytes(),
          StandardCharsets.UTF_8);
    }
  }

  @Configuration
  @EnableWebMvc
  @EnableTransactionManagement
  @Import({
      SecurityConfig.class,
      JwtService.class,
      AvaliarColetaService.class,
      AvaliacaoColetaGatewayImpl.class,
      AvaliacaoColetaController.class,
      AvaliacaoColetaMapper.class,
      AvaliacaoColetaExceptionHandler.class
  })
  static class Config {

    @Bean
    DataSource dataSource() {
      return new DriverManagerDataSource(
          POSTGRES.getJdbcUrl(),
          POSTGRES.getUsername(),
          POSTGRES.getPassword());
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    @Bean
    PlatformTransactionManager transactionManager(
        DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }
  }
}
