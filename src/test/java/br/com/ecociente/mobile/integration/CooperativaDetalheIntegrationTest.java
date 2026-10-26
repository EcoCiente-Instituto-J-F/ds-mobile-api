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
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
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
import br.com.ecociente.mobile.core.service.ConsultarCooperativaService;
import br.com.ecociente.mobile.dataprovider.entity.CooperativaDetalheEntity;
import br.com.ecociente.mobile.dataprovider.gateway.CooperativaDetalheGatewayImpl;
import br.com.ecociente.mobile.dataprovider.mapper.CooperativaDetalhePersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.CooperativaDetalheRepository;
import br.com.ecociente.mobile.entrypoint.controller.CooperativaDetalheController;
import br.com.ecociente.mobile.entrypoint.exception.ConsultaCooperativaExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.CooperativaMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.persistence.EntityManagerFactory;

@Testcontainers
@SpringJUnitConfig(CooperativaDetalheIntegrationTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
@Execution(ExecutionMode.SAME_THREAD)
class CooperativaDetalheIntegrationTest {

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

      statement.execute(recurso(
          "sql/cooperativa-detalhe-dados-test.sql"));
    }

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Test
  void deveRetornarDetalhesContatoEnderecoEAvaliacoes()
      throws Exception {

    mvc.perform(get("/api/cooperativas/1")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.nome").value("Cooperativa Um"))
        .andExpect(jsonPath("$.email").value("contato@teste.com"))
        .andExpect(jsonPath("$.telefone").value("11999990000"))
        .andExpect(jsonPath("$.cep").value("01001000"))
        .andExpect(jsonPath("$.logradouro")
            .value("Rua da Cooperativa"))
        .andExpect(jsonPath("$.numero").value("100"))
        .andExpect(jsonPath("$.enderecoCompleto").isNotEmpty())
        .andExpect(jsonPath("$.mediaAvaliacoes").value(4.0))
        .andExpect(jsonPath("$.totalAvaliacoes").value(3));
  }

  @Test
  void deveCalcularAvaliacoesSomenteDaCooperativaConsultada()
      throws Exception {

    mvc.perform(get("/api/cooperativas/2")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2))
        .andExpect(jsonPath("$.mediaAvaliacoes").value(1.0))
        .andExpect(jsonPath("$.totalAvaliacoes").value(1));
  }

  @Test
  void deveAceitarCidadeEEstadoComCaixaEEspacosDiferentes()
      throws Exception {

    // Endereço da cooperativa 1: " são paulo " / " sp ".
    // Endereço dos condomínios: "São Paulo" / "SP".
    mvc.perform(get("/api/cooperativas/1")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(ints = { 3, 4, 5, 999 })
  void deveOcultarCooperativaForaDaRegiaoInativaOuInexistente(
      int cooperativaId) throws Exception {

    mvc.perform(get("/api/cooperativas/{id}", cooperativaId)
        .header("Authorization", bearer(30)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo")
            .value("COOPERATIVA_NAO_ENCONTRADA"));
  }

  @Test
  void deveRetornarMediaNulaQuandoNaoHaAvaliacoes()
      throws Exception {

    // Remove apenas as avaliações da cooperativa 1.
    jdbc.update("""
        DELETE FROM tb_avaliacoes_visitas_coletas
        WHERE visita_coleta_id IN (
            SELECT v.id_visita_coleta
            FROM tb_visitas_coletas v
            JOIN tb_agendamentos_coletas ag
              ON ag.id_agendamento_coleta = v.agendamento_coleta_id
            WHERE ag.cooperativa_id = 1
        )
        """);

    mvc.perform(get("/api/cooperativas/1")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mediaAvaliacoes").doesNotExist())
        .andExpect(jsonPath("$.totalAvaliacoes").value(0));
  }

  @Test
  void deveOcultarCooperativaSemEndereco()
      throws Exception {

    jdbc.update("""
        UPDATE tb_cooperativas
        SET endereco_id = NULL
        WHERE id_cooperativa = 1
        """);

    mvc.perform(get("/api/cooperativas/1")
        .header("Authorization", bearer(30)))
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(ints = { 10, 20, 40 })
  void deveBloquearUsuarioQueNaoEhSindicoResponsavel(
      int usuario) throws Exception {

    mvc.perform(get("/api/cooperativas/1")
        .header("Authorization", bearer(usuario)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo")
            .value("SINDICO_SEM_CONDOMINIO"));
  }

  @Test
  void deveBloquearSindicoInativo() throws Exception {
    jdbc.update("""
        UPDATE tb_usuarios
        SET ativo = FALSE
        WHERE id_usuario = 30
        """);

    mvc.perform(get("/api/cooperativas/1")
        .header("Authorization", bearer(30)))
        .andExpect(status().isForbidden());
  }

  @Test
  void deveBloquearSindicoSemCondominioAtivo()
      throws Exception {

    jdbc.update("""
        UPDATE tb_condominios
        SET ativo = FALSE
        WHERE sindico_id = 1
        """);

    mvc.perform(get("/api/cooperativas/1")
        .header("Authorization", bearer(30)))
        .andExpect(status().isForbidden());
  }

  @Test
  void devePermitirCooperativaSemResponsavelConformeListagem()
      throws Exception {

    jdbc.update("""
        UPDATE tb_cooperativas
        SET usuario_id = NULL
        WHERE id_cooperativa = 1
        """);

    mvc.perform(get("/api/cooperativas/1")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk());
  }

  @Test
  void deveExigirTokenValido() throws Exception {
    mvc.perform(get("/api/cooperativas/1"))
        .andExpect(status().isUnauthorized());

    mvc.perform(get("/api/cooperativas/1")
        .header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "-1", "abc" })
  void deveRejeitarIdInvalido(String id)
      throws Exception {

    mvc.perform(get("/api/cooperativas/{id}", id)
        .header("Authorization", bearer(30)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void consultaNaoDeveAlterarDados() throws Exception {
    var antes = jdbc.queryForList("""
        SELECT id_cooperativa, nome_cooperativa,
               email_cooperativa, telefone_cooperativa,
               usuario_id, endereco_id
        FROM tb_cooperativas
        ORDER BY id_cooperativa
        """);

    mvc.perform(get("/api/cooperativas/1")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk());

    var depois = jdbc.queryForList("""
        SELECT id_cooperativa, nome_cooperativa,
               email_cooperativa, telefone_cooperativa,
               usuario_id, endereco_id
        FROM tb_cooperativas
        ORDER BY id_cooperativa
        """);

    assertEquals(antes, depois);
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
  @EnableJpaRepositories(basePackageClasses = CooperativaDetalheRepository.class, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CooperativaDetalheRepository.class))
  @Import({
      SecurityConfig.class,
      JwtService.class,
      ConsultarCooperativaService.class,
      CooperativaDetalheGatewayImpl.class,
      CooperativaDetalhePersistenceMapper.class,
      CooperativaDetalheController.class,
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
    LocalContainerEntityManagerFactoryBean entityManagerFactory(
        DataSource dataSource) {

      var factory = new LocalContainerEntityManagerFactoryBean();

      factory.setDataSource(dataSource);
      factory.setJpaVendorAdapter(
          new HibernateJpaVendorAdapter());

      factory.setManagedTypes(PersistenceManagedTypes.of(
          CooperativaDetalheEntity.class.getName()));

      factory.setJpaPropertyMap(Map.of(
          "hibernate.hbm2ddl.auto", "none"));

      return factory;
    }

    @Bean
    PlatformTransactionManager transactionManager(
        EntityManagerFactory entityManagerFactory) {
      return new JpaTransactionManager(
          entityManagerFactory);
    }
  }
}