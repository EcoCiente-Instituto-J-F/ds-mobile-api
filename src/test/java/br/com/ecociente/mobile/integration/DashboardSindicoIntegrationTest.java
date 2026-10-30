package br.com.ecociente.mobile.integration;

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
import br.com.ecociente.mobile.core.service.ConsultarDashboardSindicoService;
import br.com.ecociente.mobile.dataprovider.gateway.DashboardSindicoGatewayImpl;
import br.com.ecociente.mobile.entrypoint.controller.DashboardSindicoController;
import br.com.ecociente.mobile.entrypoint.exception.DashboardSindicoExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.DashboardSindicoMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Testcontainers
@SpringJUnitConfig(DashboardSindicoIntegrationTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
@Execution(ExecutionMode.SAME_THREAD)
class DashboardSindicoIntegrationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.com.ecociente.mobile.config.security.UsuarioDetailsService
            usuarioDetailsServiceTeste;


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
          "sql/dashboard-sindico-dados-test.sql"));
    }

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Test
  void deveDevolverIdentificacaoEMoradores() throws Exception {
    mvc.perform(get("/api/condominios/1/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.condominioId").value(1))
        .andExpect(jsonPath("$.nomeCondominio").value("Condomínio Um"))
        .andExpect(jsonPath("$.nomeSindico").value("Síndico Um"))
        .andExpect(jsonPath("$.totalMoradores").value(2))
        .andExpect(jsonPath("$.sindicoUsuarioId").doesNotExist());
  }

  @Test
  void deveContarOsMateriaisPorSituacao() throws Exception {
    mvc.perform(get("/api/condominios/1/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.materiais.reciclados").value(3))
        .andExpect(jsonPath("$.materiais.emValidacao").value(2))
        .andExpect(jsonPath("$.materiais.aguardandoSindico").value(1))
        .andExpect(jsonPath("$.materiais.reprovadas").value(1));
  }

  @Test
  void deveAgruparMateriaisPorCategoriaDoMaiorParaOMenor() throws Exception {
    mvc.perform(get("/api/condominios/1/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.materiais.porCategoria.length()").value(2))
        .andExpect(jsonPath("$.materiais.porCategoria[0].categoriaId").value(1))
        .andExpect(jsonPath("$.materiais.porCategoria[0].categoria").value("Plástico"))
        .andExpect(jsonPath("$.materiais.porCategoria[0].cor").value("#E53935"))
        .andExpect(jsonPath("$.materiais.porCategoria[0].quantidade").value(2))
        .andExpect(jsonPath("$.materiais.porCategoria[1].categoria").value("Papel"))
        .andExpect(jsonPath("$.materiais.porCategoria[1].quantidade").value(1));
  }

  @Test
  void deveSomarOsPontosDescontandoEstornos() throws Exception {
    mvc.perform(get("/api/condominios/1/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pontos.total").value(40))
        .andExpect(jsonPath("$.pontos.postagens").value(10))
        .andExpect(jsonPath("$.pontos.quizzes").value(30))
        .andExpect(jsonPath("$.pontos.mesAtual").value(40));
  }

  @Test
  void deveResumirOsQuizzesConcluidos() throws Exception {
    mvc.perform(get("/api/condominios/1/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quizzes.tentativasConcluidas").value(3))
        .andExpect(jsonPath("$.quizzes.tentativasAprovadas").value(2))
        .andExpect(jsonPath("$.quizzes.moradoresComQuiz").value(2))
        .andExpect(jsonPath("$.quizzes.notaMedia").value(73.33))
        .andExpect(jsonPath("$.quizzes.taxaAprovacao").value(66.67));
  }

  @Test
  void deveDetalharNotasPorQuizEmOrdemAlfabetica() throws Exception {
    mvc.perform(get("/api/condominios/1/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quizzes.porQuiz.length()").value(2))
        .andExpect(jsonPath("$.quizzes.porQuiz[0].quizId").value(101))
        .andExpect(jsonPath("$.quizzes.porQuiz[0].quiz").value("Quiz Dois"))
        .andExpect(jsonPath("$.quizzes.porQuiz[0].tentativas").value(1))
        .andExpect(jsonPath("$.quizzes.porQuiz[0].aprovadas").value(1))
        .andExpect(jsonPath("$.quizzes.porQuiz[0].notaMedia").value(80.0))
        .andExpect(jsonPath("$.quizzes.porQuiz[1].quiz").value("Quiz Um"))
        .andExpect(jsonPath("$.quizzes.porQuiz[1].tentativas").value(2))
        .andExpect(jsonPath("$.quizzes.porQuiz[1].aprovadas").value(1))
        .andExpect(jsonPath("$.quizzes.porQuiz[1].notaMedia").value(70.0));
  }

  @Test
  void deveIsolarOsNumerosEntreCondominios() throws Exception {
    mvc.perform(get("/api/condominios/2/dashboard")
        .header("Authorization", bearer(11)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalMoradores").value(1))
        .andExpect(jsonPath("$.materiais.reciclados").value(1))
        .andExpect(jsonPath("$.pontos.total").value(99))
        .andExpect(jsonPath("$.quizzes.tentativasConcluidas").value(1))
        .andExpect(jsonPath("$.quizzes.notaMedia").value(100.0));
  }

  @Test
  void condominioSemDadosDeveDevolverZerosEListasVazias() throws Exception {
    mvc.perform(get("/api/condominios/3/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalMoradores").value(0))
        .andExpect(jsonPath("$.materiais.reciclados").value(0))
        .andExpect(jsonPath("$.materiais.porCategoria").isEmpty())
        .andExpect(jsonPath("$.pontos.total").value(0))
        .andExpect(jsonPath("$.pontos.mesAtual").value(0))
        .andExpect(jsonPath("$.quizzes.tentativasConcluidas").value(0))
        .andExpect(jsonPath("$.quizzes.notaMedia").doesNotExist())
        .andExpect(jsonPath("$.quizzes.taxaAprovacao").doesNotExist())
        .andExpect(jsonPath("$.quizzes.porQuiz").isEmpty());
  }

  @ParameterizedTest
  @ValueSource(ints = { 11, 20, 30, 40 })
  void soOSindicoResponsavelPodeConsultar(int usuario) throws Exception {
    mvc.perform(get("/api/condominios/1/dashboard")
        .header("Authorization", bearer(usuario)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("DASHBOARD_NAO_PERMITIDO"));
  }

  @Test
  void condominioSemSindicoNaoDeveSerConsultado() throws Exception {
    mvc.perform(get("/api/condominios/4/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isForbidden());
  }

  @Test
  void deveRetornar404ParaCondominioInexistente() throws Exception {
    mvc.perform(get("/api/condominios/999/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("CONDOMINIO_NAO_ENCONTRADO"));
  }

  @Test
  void deveRejeitarIdInvalido() throws Exception {
    mvc.perform(get("/api/condominios/0/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CONDOMINIO_ID_INVALIDO"));

    mvc.perform(get("/api/condominios/abc/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
  }

  @Test
  void deveExigirJwt() throws Exception {
    mvc.perform(get("/api/condominios/1/dashboard"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void consultaNaoDeveAlterarDados() throws Exception {
    Long antes = jdbc.queryForObject(
        "SELECT COUNT(*) FROM tb_postagens", Long.class);

    mvc.perform(get("/api/condominios/1/dashboard")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk());

    org.junit.jupiter.api.Assertions.assertEquals(
        antes,
        jdbc.queryForObject("SELECT COUNT(*) FROM tb_postagens", Long.class));
  }

  private String bearer(int usuario) {
    String perfil = usuario == 10 || usuario == 11
        ? "SINDICO"
        : "MORADOR";

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
      ConsultarDashboardSindicoService.class,
      DashboardSindicoGatewayImpl.class,
      DashboardSindicoController.class,
      DashboardSindicoMapper.class,
      DashboardSindicoExceptionHandler.class
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
