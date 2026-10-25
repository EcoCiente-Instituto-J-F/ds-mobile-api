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
import br.com.ecociente.mobile.core.service.ConsultarCooperativasProximasService;
import br.com.ecociente.mobile.dataprovider.gateway.CooperativaGatewayImpl;
import br.com.ecociente.mobile.entrypoint.controller.CooperativaController;
import br.com.ecociente.mobile.entrypoint.exception.ConsultaCooperativaExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.CooperativaMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Testcontainers
@SpringJUnitConfig(CooperativaIntegrationTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
@Execution(ExecutionMode.SAME_THREAD)
class CooperativaIntegrationTest {

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
          "sql/cooperativas-proximas-dados-test.sql"));
    }

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Test
  void deveListarSoAsCooperativasDaMesmaCidadeDoCondominio() throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.pagina").value(0))
        .andExpect(jsonPath("$.tamanho").value(10))
        .andExpect(jsonPath("$.totalPaginas").value(1))
        .andExpect(jsonPath("$.cooperativas.length()").value(2))
        .andExpect(jsonPath("$.cooperativas[0].id").value(1))
        .andExpect(jsonPath("$.cooperativas[0].nome").value("Cooperativa A"))
        .andExpect(jsonPath("$.cooperativas[1].id").value(4));
  }

  @Test
  void deveMontarOEnderecoEmUmaLinhaParaOMapa() throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cooperativas[0].enderecoCompleto")
            .value("Avenida Coop A 100, Galpão 2, São Paulo - SP, 01310-100"))
        .andExpect(jsonPath("$.cooperativas[0].cep").value("01310100"))
        .andExpect(jsonPath("$.cooperativas[0].telefone").value("11999990000"))
        .andExpect(jsonPath("$.cooperativas[1].enderecoCompleto")
            .value("Rua Coop D 400, são paulo - sp, 01001-002"));
  }

  @Test
  void deveInformarMediaETotalDeAvaliacoes() throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cooperativas[0].mediaAvaliacoes").value(4.0))
        .andExpect(jsonPath("$.cooperativas[0].totalAvaliacoes").value(2))
        .andExpect(jsonPath("$.cooperativas[1].mediaAvaliacoes").doesNotExist())
        .andExpect(jsonPath("$.cooperativas[1].totalAvaliacoes").value(0));
  }

  @Test
  void naoDeveListarCooperativaDeOutraCidadeInativaOuSemEndereco() throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cooperativas[?(@.id == 2)]").isEmpty())
        .andExpect(jsonPath("$.cooperativas[?(@.id == 3)]").isEmpty())
        .andExpect(jsonPath("$.cooperativas[?(@.id == 5)]").isEmpty());
  }

  @Test
  void deveListarSoAsDaCidadeDoSindicoDoRio() throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(70)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.totalPaginas").value(0))
        .andExpect(jsonPath("$.cooperativas").isEmpty());
  }

  @Test
  void devePaginarOsResultados() throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(30))
        .param("page", "1")
        .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.totalPaginas").value(2))
        .andExpect(jsonPath("$.cooperativas.length()").value(1))
        .andExpect(jsonPath("$.cooperativas[0].id").value(4));
  }

  @Test
  void naoDeveListarParaSindicoSemCondominioAtivo() throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(60)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("SINDICO_SEM_CONDOMINIO"));
  }

  @Test
  void naoDeveListarParaQuemNaoEhSindico() throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(10)))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "-1", "101", "abc" })
  void deveRejeitarTamanhoInvalido(String tamanho) throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(30))
        .param("size", tamanho))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deveRejeitarPaginaNegativa() throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(30))
        .param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deveExigirJwt() throws Exception {
    mvc.perform(get("/api/cooperativas"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRejeitarTokenInvalido() throws Exception {
    mvc.perform(get("/api/cooperativas")
        .header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void consultaNaoDeveAlterarDados() throws Exception {
    Long antes = jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_cooperativas",
        Long.class);

    mvc.perform(get("/api/cooperativas")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk());

    assertEquals(
        antes,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM tb_cooperativas",
            Long.class));
  }

  private String bearer(int usuario) {
    String perfil = usuario == 10 || usuario == 20 || usuario == 50
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
      ConsultarCooperativasProximasService.class,
      CooperativaGatewayImpl.class,
      CooperativaController.class,
      CooperativaMapper.class,
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
