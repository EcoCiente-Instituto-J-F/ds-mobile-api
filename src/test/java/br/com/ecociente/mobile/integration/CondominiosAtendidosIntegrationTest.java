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
import br.com.ecociente.mobile.core.service.ConsultarCondominiosAtendidosService;
import br.com.ecociente.mobile.dataprovider.gateway.AvaliacaoCooperativaGatewayImpl;
import br.com.ecociente.mobile.dataprovider.gateway.CondominioAtendidoGatewayImpl;
import br.com.ecociente.mobile.entrypoint.controller.CondominiosAtendidosController;
import br.com.ecociente.mobile.entrypoint.exception.ConsultaCooperativaExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.CondominiosAtendidosMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Testcontainers
@SpringJUnitConfig(CondominiosAtendidosIntegrationTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
@Execution(ExecutionMode.SAME_THREAD)
class CondominiosAtendidosIntegrationTest {
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
          "sql/avaliacoes-cooperativa-dados-test.sql"));
    }

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Test
  void deveListarSemDuplicarCondominiosComEnderecoEDias() throws Exception {
    jdbc.update("INSERT INTO tb_torres(nome_torre, condominio_id) VALUES ('A',1), ('B',1)");
    jdbc.update("INSERT INTO tb_lkp_status_agendamentos(nome_status) VALUES ('Confirmado')");
    jdbc.update("INSERT INTO tb_lkp_dias_semanas(nome_dia) VALUES ('Segunda-feira')");
    jdbc.update("""
        UPDATE tb_agendamentos_coletas SET possui_recorrencia = TRUE,
          status_agendamento_id = (SELECT MAX(id_status) FROM tb_lkp_status_agendamentos)
        WHERE id_agendamento_coleta IN (100, 102)
        """);
    jdbc.update("""
        INSERT INTO tb_rel_recorrencias_agendamentos(agendamento_coleta_id, dia_semana_id)
        SELECT ag.id_agendamento_coleta, d.id_dia_semana
        FROM tb_agendamentos_coletas ag CROSS JOIN tb_lkp_dias_semanas d
        WHERE ag.id_agendamento_coleta IN (100, 102) AND d.nome_dia = 'Segunda-feira'
        """);
    mvc.perform(get("/api/cooperativas/1/condominios").header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.condominios.length()").value(2))
        .andExpect(jsonPath("$.condominios[0].nome").value("Condomínio Dois"))
        .andExpect(jsonPath("$.condominios[1].totalTorres").value(2))
        .andExpect(jsonPath("$.condominios[1].endereco.logradouro").value("Rua Um"))
        .andExpect(jsonPath("$.condominios[1].diasColeta.length()").value(1))
        .andExpect(jsonPath("$.condominios[1].diasColeta[0]").value("Segunda-feira"));
  }

  @Test
  void deveExcluirColetasPendentesRecusadasCanceladasECondominiosInativos() throws Exception {
    for (String nome : new String[]{"Agendado", "Recusado", "Cancelado"}) {
      jdbc.update("INSERT INTO tb_lkp_status_agendamentos(nome_status) VALUES (?)", nome);
      jdbc.update("""
          UPDATE tb_agendamentos_coletas SET status_agendamento_id =
            (SELECT MAX(id_status) FROM tb_lkp_status_agendamentos) WHERE cooperativa_id = 1
          """);
      mvc.perform(get("/api/cooperativas/1/condominios").header("Authorization", bearer(10)))
          .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
    jdbc.update("UPDATE tb_condominios SET ativo = FALSE");
    mvc.perform(get("/api/cooperativas/2/condominios").header("Authorization", bearer(20)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.condominios").isEmpty());
  }

  @Test
  void deveIsolarCooperativasEPaginarPreservandoResumo() throws Exception {
    mvc.perform(get("/api/cooperativas/2/condominios").header("Authorization", bearer(20)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.condominios[0].id").value(1));
    mvc.perform(get("/api/cooperativas/1/condominios").header("Authorization", bearer(10))
        .param("size", "1").param("page", "1"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.totalPaginas").value(2))
        .andExpect(jsonPath("$.condominios[0].id").value(1));
    mvc.perform(get("/api/cooperativas/1/condominios").header("Authorization", bearer(10))
        .param("page", "99"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.condominios").isEmpty());
  }

  @Test
  void deveValidarIdentidadeEParametros() throws Exception {
    mvc.perform(get("/api/cooperativas/1/condominios")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/cooperativas/1/condominios").header("Authorization", bearer(20)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/cooperativas/999/condominios").header("Authorization", bearer(10)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/cooperativas/4/condominios").header("Authorization", bearer(50)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/cooperativas/abc/condominios").header("Authorization", bearer(10)))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/cooperativas/1/condominios").header("Authorization", bearer(10))
        .param("size", "101")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/cooperativas/3/condominios").header("Authorization", bearer(40)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
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
      ConsultarCondominiosAtendidosService.class,
      AvaliacaoCooperativaGatewayImpl.class,
      CondominioAtendidoGatewayImpl.class,
      CondominiosAtendidosController.class,
      CondominiosAtendidosMapper.class,
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