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
import br.com.ecociente.mobile.core.service.ConsultarPostagensEmAnaliseService;
import br.com.ecociente.mobile.dataprovider.entity.PostagemEntity;
import br.com.ecociente.mobile.dataprovider.gateway.PostagemAnaliseGatewayImpl;
import br.com.ecociente.mobile.dataprovider.mapper.PostagemAnalisePersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.PostagemAnaliseRepository;
import br.com.ecociente.mobile.entrypoint.controller.PostagensEmAnaliseController;
import br.com.ecociente.mobile.entrypoint.exception.PostagemExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.PostagemMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.persistence.EntityManagerFactory;

@Testcontainers
@SpringJUnitConfig(PostagensEmAnaliseIntegrationTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
@Execution(ExecutionMode.SAME_THREAD)
class PostagensEmAnaliseIntegrationTest {
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
          "sql/postagens-em-analise-dados-test.sql"));
    }

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Test
  void deveRetornarSomenteEmAnaliseComDadosCompletos()
      throws Exception {

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.totalPaginas").value(1))
        .andExpect(jsonPath("$.postagens.length()").value(3))
        .andExpect(jsonPath("$.postagens[0].id").value(100))
        .andExpect(jsonPath("$.postagens[0].usuarioId").value(50))
        .andExpect(jsonPath("$.postagens[0].autor")
            .value("Morador Teste"))
        .andExpect(jsonPath("$.postagens[0].condominioId").value(1))
        .andExpect(jsonPath("$.postagens[0].torreId").value(1))
        .andExpect(jsonPath("$.postagens[0].categoria")
            .value("Plástico"))
        .andExpect(jsonPath("$.postagens[0].statusValidacao")
            .value("em_analise"))
        .andExpect(jsonPath("$.postagens[0].saldoConfianca")
            .value(-2))
        .andExpect(jsonPath("$.postagens[0].urlFoto")
            .value("https://img.teste/postagem-100.jpg"))
        .andExpect(jsonPath("$.postagens[0].capturadaEm").exists())
        .andExpect(jsonPath("$.postagens[0].dataPostagem").exists())
        .andExpect(jsonPath("$.postagens[0].dataLimiteAnalise")
            .exists());
  }

  @Test
  void deveIncluirEmAnaliseMesmoComResolvidoEmPreenchido()
      throws Exception {

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.postagens[0].id").value(100))
        .andExpect(jsonPath("$.postagens[0].resolvidoEm").exists())
        .andExpect(jsonPath("$.postagens[1].id").value(101))
        .andExpect(jsonPath("$.postagens[1].torreId").doesNotExist())
        .andExpect(jsonPath("$.postagens[1].resolvidoEm")
            .doesNotExist())
        .andExpect(jsonPath("$.postagens[2].id").value(102));
  }

  @Test
  void devePaginarEDesempatarOrdenacaoPeloId()
      throws Exception {

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", bearer(30))
        .param("page", "0")
        .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.totalPaginas").value(2))
        .andExpect(jsonPath("$.postagens.length()").value(2))
        .andExpect(jsonPath("$.postagens[0].id").value(100))
        .andExpect(jsonPath("$.postagens[1].id").value(101));

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", bearer(30))
        .param("page", "1")
        .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.postagens.length()").value(1))
        .andExpect(jsonPath("$.postagens[0].id").value(102));
  }

  @Test
  void devePreservarTotalNaPaginaAlemDoFinal()
      throws Exception {

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", bearer(30))
        .param("page", "99")
        .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.totalPaginas").value(2))
        .andExpect(jsonPath("$.postagens").isEmpty());
  }

  @Test
  void deveIsolarPostagensEntreCondominios()
      throws Exception {

    mvc.perform(get("/api/condominios/2/postagens/em-analise")
        .header("Authorization", bearer(40)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.postagens.length()").value(1))
        .andExpect(jsonPath("$.postagens[0].id").value(200))
        .andExpect(jsonPath("$.postagens[0].condominioId").value(2));
  }

  @ParameterizedTest
  @ValueSource(ints = { 40, 50 })
  void deveBloquearOutroSindicoEMorador(int usuario)
      throws Exception {

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", bearer(usuario)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo")
            .value("POSTAGENS_EM_ANALISE_NAO_PERMITIDAS"));
  }

  @Test
  void deveRetornarListaVaziaSemPostagens()
      throws Exception {

    mvc.perform(get("/api/condominios/3/postagens/em-analise")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.totalPaginas").value(0))
        .andExpect(jsonPath("$.postagens").isEmpty());
  }

  @ParameterizedTest
  @ValueSource(ints = { 4, 5, 6, 999 })
  void deveRetornar404ParaCondominioIndisponivel(
      int condominioId) throws Exception {

    mvc.perform(get(
        "/api/condominios/{id}/postagens/em-analise",
        condominioId)
        .header("Authorization", bearer(30)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo")
            .value("CONDOMINIO_INDISPONIVEL"));
  }

  @Test
  void deveBloquearSindicoQueFoiInativado()
      throws Exception {

    jdbc.update("""
        UPDATE tb_usuarios
        SET ativo = FALSE
        WHERE id_usuario = 30
        """);

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", bearer(30)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveExigirTokenValido() throws Exception {
    mvc.perform(get("/api/condominios/1/postagens/em-analise"))
        .andExpect(status().isUnauthorized());

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "-1", "101", "abc" })
  void deveRejeitarTamanhoInvalido(String tamanho)
      throws Exception {

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", bearer(30))
        .param("size", tamanho))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deveRejeitarIdEPaginaInvalidos() throws Exception {
    mvc.perform(get("/api/condominios/0/postagens/em-analise")
        .header("Authorization", bearer(30)))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/condominios/abc/postagens/em-analise")
        .header("Authorization", bearer(30)))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", bearer(30))
        .param("page", "-1"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void consultaNaoDeveAlterarPostagens() throws Exception {
    var antes = jdbc.queryForList("""
        SELECT id_postagem, status_validacao_id,
               saldo_confianca, resolvido_em
        FROM tb_postagens
        ORDER BY id_postagem
        """);

    mvc.perform(get("/api/condominios/1/postagens/em-analise")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk());

    var depois = jdbc.queryForList("""
        SELECT id_postagem, status_validacao_id,
               saldo_confianca, resolvido_em
        FROM tb_postagens
        ORDER BY id_postagem
        """);

    assertEquals(antes, depois);
  }

  private String bearer(int usuario) {
    String perfil = usuario == 50
        ? "MORADOR"
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
  @EnableJpaRepositories(basePackageClasses = PostagemAnaliseRepository.class, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PostagemAnaliseRepository.class))
  @Import({
      SecurityConfig.class,
      JwtService.class,
      ConsultarPostagensEmAnaliseService.class,
      PostagemAnaliseGatewayImpl.class,
      PostagemAnalisePersistenceMapper.class,
      PostagensEmAnaliseController.class,
      PostagemMapper.class,
      PostagemExceptionHandler.class
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
          PostagemEntity.class.getName()));

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