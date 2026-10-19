package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import br.com.ecociente.mobile.core.service.ConsultarAvaliacoesCooperativaService;
import br.com.ecociente.mobile.dataprovider.gateway.AvaliacaoCooperativaGatewayImpl;
import br.com.ecociente.mobile.entrypoint.controller.AvaliacoesCooperativaController;
import br.com.ecociente.mobile.entrypoint.exception.ConsultaCooperativaExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.AvaliacoesCooperativaMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Testcontainers
@SpringJUnitConfig(AvaliacoesCooperativaIntegrationTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
@Execution(ExecutionMode.SAME_THREAD)
class AvaliacoesCooperativaIntegrationTest {

  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  WebApplicationContext context;

  @Autowired
  DataSource dataSource;

  @Autowired
  JdbcTemplate jdbc;

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
          "sql/avaliacoes-cooperativa-dados-test.sql"));
    }

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Test
  void deveRetornarAvaliacoesResumoEDadosDaVisita()
      throws Exception {

    mvc.perform(get("/api/cooperativas/1/avaliacoes")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cooperativaId").value(1))
        .andExpect(jsonPath("$.media").value(4.0))
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.pagina").value(0))
        .andExpect(jsonPath("$.tamanho").value(10))
        .andExpect(jsonPath("$.totalPaginas").value(1))
        .andExpect(jsonPath("$.avaliacoes.length()").value(3))
        .andExpect(jsonPath("$.avaliacoes[0].id").value(1))
        .andExpect(jsonPath("$.avaliacoes[0].visitaId").value(1000))
        .andExpect(jsonPath("$.avaliacoes[0].agendamentoId").value(100))
        .andExpect(jsonPath("$.avaliacoes[0].condominioId").value(1))
        .andExpect(jsonPath("$.avaliacoes[0].nomeCondominio")
            .value("Condomínio Um"))
        .andExpect(jsonPath("$.avaliacoes[0].nota").value(5))
        .andExpect(jsonPath("$.avaliacoes[0].comentario")
            .value("Excelente atendimento"))
        .andExpect(jsonPath("$.avaliacoes[0].avaliadoEm").exists());
  }

  @Test
  void devePaginarSemAlterarResumoGeral() throws Exception {
    mvc.perform(get("/api/cooperativas/1/avaliacoes")
        .header("Authorization", bearer(10))
        .param("page", "0")
        .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.media").value(4.0))
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.totalPaginas").value(2))
        .andExpect(jsonPath("$.avaliacoes.length()").value(2))
        .andExpect(jsonPath("$.avaliacoes[0].id").value(1))
        .andExpect(jsonPath("$.avaliacoes[1].id").value(2));

    mvc.perform(get("/api/cooperativas/1/avaliacoes")
        .header("Authorization", bearer(10))
        .param("page", "1")
        .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.media").value(4.0))
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.avaliacoes.length()").value(1))
        .andExpect(jsonPath("$.avaliacoes[0].id").value(3));
  }

  @Test
  void deveRetornarPaginaVaziaAlemDoFinal() throws Exception {
    mvc.perform(get("/api/cooperativas/1/avaliacoes")
        .header("Authorization", bearer(10))
        .param("page", "99")
        .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.media").value(4.0))
        .andExpect(jsonPath("$.avaliacoes").isEmpty());
  }

  @Test
  void deveDesempatarOrdenacaoPeloId() throws Exception {
    jdbc.update("""
        UPDATE tb_avaliacoes_visitas_coletas
        SET avaliado_em = TIMESTAMPTZ '2026-01-04 12:00:00-03'
        WHERE id_avaliacao IN (1, 2, 3)
        """);

    mvc.perform(get("/api/cooperativas/1/avaliacoes")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.avaliacoes[0].id").value(3))
        .andExpect(jsonPath("$.avaliacoes[1].id").value(2))
        .andExpect(jsonPath("$.avaliacoes[2].id").value(1));
  }

  @Test
  void deveIsolarAvaliacoesEntreCooperativas() throws Exception {
    mvc.perform(get("/api/cooperativas/2/avaliacoes")
        .header("Authorization", bearer(20)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.media").value(1.0))
        .andExpect(jsonPath("$.avaliacoes.length()").value(1))
        .andExpect(jsonPath("$.avaliacoes[0].id").value(4));
  }

  @ParameterizedTest
  @ValueSource(ints = { 20, 30, 40 })
  void outroUsuarioNaoDeveConsultarCooperativa(
      int usuario) throws Exception {

    mvc.perform(get("/api/cooperativas/1/avaliacoes")
        .header("Authorization", bearer(usuario)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo")
            .value("AVALIACOES_NAO_PERMITIDAS"));
  }

  @Test
  void cooperativaSemAvaliacoesDeveRetornarResumoVazio()
      throws Exception {

    mvc.perform(get("/api/cooperativas/3/avaliacoes")
        .header("Authorization", bearer(40)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.totalPaginas").value(0))
        .andExpect(jsonPath("$.media").doesNotExist())
        .andExpect(jsonPath("$.avaliacoes").isEmpty());
  }

  @Test
  void deveBloquearResponsavelInativo() throws Exception {
    mvc.perform(get("/api/cooperativas/4/avaliacoes")
        .header("Authorization", bearer(50)))
        .andExpect(status().isNotFound());

    // Comportamento definido pelo service:
    // sem responsável ativo, a cooperativa fica indisponível.
  }

  @Test
  void deveRetornar404ParaCooperativaInexistente()
      throws Exception {

    mvc.perform(get("/api/cooperativas/999/avaliacoes")
        .header("Authorization", bearer(10)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveRejeitarIdInvalido() throws Exception {
    mvc.perform(get("/api/cooperativas/0/avaliacoes")
        .header("Authorization", bearer(10)))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/cooperativas/abc/avaliacoes")
        .header("Authorization", bearer(10)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo")
            .value("PARAMETRO_INVALIDO"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "-1", "101", "abc" })
  void deveRejeitarTamanhoInvalido(String tamanho)
      throws Exception {

    mvc.perform(get("/api/cooperativas/1/avaliacoes")
        .header("Authorization", bearer(10))
        .param("size", tamanho))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deveRejeitarPaginaNegativa() throws Exception {
    mvc.perform(get("/api/cooperativas/1/avaliacoes")
        .header("Authorization", bearer(10))
        .param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deveExigirJwt() throws Exception {
    mvc.perform(get("/api/cooperativas/1/avaliacoes"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRejeitarTokenInvalido() throws Exception {
    mvc.perform(get("/api/cooperativas/1/avaliacoes")
        .header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void consultaNaoDeveAlterarDados() throws Exception {
    Long antes = jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_avaliacoes_visitas_coletas",
        Long.class);

    mvc.perform(get("/api/cooperativas/1/avaliacoes")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk());

    assertEquals(
        antes,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM tb_avaliacoes_visitas_coletas",
            Long.class));
  }

  private String bearer(int usuario) {
    String perfil = usuario == 30
        ? "SINDICO"
        : "COOPERATIVA";

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
      ConsultarAvaliacoesCooperativaService.class,
      AvaliacaoCooperativaGatewayImpl.class,
      AvaliacoesCooperativaController.class,
      AvaliacoesCooperativaMapper.class,
      ConsultaCooperativaExceptionHandler.class
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