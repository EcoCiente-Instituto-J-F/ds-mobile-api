package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ecociente.mobile.config.security.JwtService;
import br.com.ecociente.mobile.config.security.SecurityConfig;
import br.com.ecociente.mobile.core.service.ChatService;
import br.com.ecociente.mobile.dataprovider.entity.ConversaEntity;
import br.com.ecociente.mobile.dataprovider.entity.MensagemConversaEntity;
import br.com.ecociente.mobile.dataprovider.gateway.ChatGatewayImpl;
import br.com.ecociente.mobile.dataprovider.mapper.ChatPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.ConversaRepository;
import br.com.ecociente.mobile.dataprovider.repository.MensagemConversaRepository;
import br.com.ecociente.mobile.entrypoint.controller.ChatController;
import br.com.ecociente.mobile.entrypoint.exception.ChatExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.ChatMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.persistence.EntityManagerFactory;

@Testcontainers
@SpringJUnitConfig(ChatIntegrationTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
@Execution(ExecutionMode.SAME_THREAD)
class ChatIntegrationTest {
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

  @Autowired
  ChatService service;

  private final ObjectMapper json = new ObjectMapper();

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
          "sql/chat-dados-test.sql"));
    }

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Test
  void deveCriarOuRecuperarConversaSemDuplicar() throws Exception {
    int primeira = criar(30, 1, 1);
    int segunda = criar(10, 1, 1);

    assertEquals(primeira, segunda);

    assertEquals(
        1L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM tb_conversas",
            Long.class));
  }

  @Test
  void deveCriarUmaConversaMesmoComChamadasConcorrentes()
      throws Exception {

    try (var executor = Executors.newFixedThreadPool(2)) {
      var primeira = executor.submit(
          () -> service.criar(30, 1, 1).id());

      var segunda = executor.submit(
          () -> service.criar(10, 1, 1).id());

      assertEquals(
          primeira.get(15, TimeUnit.SECONDS),
          segunda.get(15, TimeUnit.SECONDS));
    }

    assertEquals(
        1L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM tb_conversas",
            Long.class));
  }

  @Test
  void deveEnviarComIdentidadeDoTokenEAtualizarUltimaMensagem()
      throws Exception {

    int conversaId = criar(30, 1, 1);

    var resultado = mvc.perform(
        post("/api/conversas/{id}/mensagens", conversaId)
            .header("Authorization", bearer(10))
            .contentType("application/json")
            .content("""
                {
                  "conteudo": "  Olá síndico!  ",
                  "remetenteUsuarioId": 30,
                  "remetenteTipo": "CONDOMINIO"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.remetenteUsuarioId").value(10))
        .andExpect(jsonPath("$.remetenteTipo").value("COOPERATIVA"))
        .andExpect(jsonPath("$.conteudo").value("Olá síndico!"))
        .andReturn();

    long mensagemId = json.readTree(
        resultado.getResponse().getContentAsString())
        .get("id")
        .asLong();

    OffsetDateTime enviada = jdbc.queryForObject(
        """
            SELECT enviada_em
            FROM tb_mensagens_conversas
            WHERE id_mensagem = ?
            """,
        OffsetDateTime.class,
        mensagemId);

    OffsetDateTime ultima = jdbc.queryForObject(
        """
            SELECT ultima_mensagem_em
            FROM tb_conversas
            WHERE id_conversa = ?
            """,
        OffsetDateTime.class,
        conversaId);

    assertNotNull(enviada);
    assertNotNull(ultima);
    assertEquals(enviada.toInstant(), ultima.toInstant());
  }

  @Test
  void deveMarcarSomenteRecebidasAteOLimiteSemAlterarLeituraAnterior()
      throws Exception {

    int conversaId = criar(30, 1, 1);

    long propria = enviar(30, conversaId, "Mensagem do síndico");
    long recebida = enviar(10, conversaId, "Primeira resposta");
    long posterior = enviar(10, conversaId, "Segunda resposta");

    mvc.perform(get("/api/conversas")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens[0].naoLidas").value(2));

    mvc.perform(patch("/api/conversas/{id}/leitura", conversaId)
        .header("Authorization", bearer(30))
        .contentType("application/json")
        .content(json.writeValueAsString(
            Map.of("ateMensagemId", recebida))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mensagensMarcadas").value(1));

    OffsetDateTime primeiraLeitura = leitura(recebida);

    assertNotNull(primeiraLeitura);
    assertNull(leitura(propria));
    assertNull(leitura(posterior));

    mvc.perform(patch("/api/conversas/{id}/leitura", conversaId)
        .header("Authorization", bearer(30))
        .contentType("application/json")
        .content(json.writeValueAsString(
            Map.of("ateMensagemId", recebida))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mensagensMarcadas").value(0));

    assertEquals(primeiraLeitura, leitura(recebida));

    mvc.perform(patch("/api/conversas/{id}/leitura", conversaId)
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mensagensMarcadas").value(1));

    assertNull(leitura(propria));
    assertNotNull(leitura(posterior));

    mvc.perform(get("/api/conversas")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens[0].naoLidas").value(0));
  }

  @Test
  void devePaginarMensagensSemMarcarLeituraAutomaticamente()
      throws Exception {

    int conversaId = criar(30, 1, 1);

    enviar(10, conversaId, "Primeira");
    enviar(10, conversaId, "Segunda");
    long terceira = enviar(10, conversaId, "Terceira");

    // Força empate para validar o desempate pelo ID.
    jdbc.update("""
        UPDATE tb_mensagens_conversas
        SET enviada_em = TIMESTAMPTZ '2026-01-01 12:00:00+00'
        WHERE conversa_id = ?
        """, conversaId);

    mvc.perform(get("/api/conversas/{id}/mensagens", conversaId)
        .header("Authorization", bearer(30))
        .param("page", "0")
        .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.totalPaginas").value(2))
        .andExpect(jsonPath("$.itens.length()").value(2))
        .andExpect(jsonPath("$.itens[0].id").value(terceira))
        .andExpect(jsonPath("$.itens[0].conteudo").value("Terceira"));

    mvc.perform(get("/api/conversas/{id}/mensagens", conversaId)
        .header("Authorization", bearer(30))
        .param("page", "99")
        .param("size", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.itens").isEmpty());

    assertEquals(
        3L,
        jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM tb_mensagens_conversas
            WHERE conversa_id = ?
              AND lida_em IS NULL
            """,
            Long.class,
            conversaId));
  }

  @Test
  void deveArquivarParaAmbosPreservarHistoricoEDesarquivar()
      throws Exception {

    int conversaId = criar(30, 1, 1);

    enviar(10, conversaId, "Histórico");

    mvc.perform(
        patch("/api/conversas/{id}/arquivamento", conversaId)
            .header("Authorization", bearer(30))
            .contentType("application/json")
            .content("""
                {"arquivada": true}
                """))
        .andExpect(status().isNoContent());

    // Repetir a criação não reativa a conversa.
    assertEquals(conversaId, criar(10, 1, 1));

    mvc.perform(get("/api/conversas")
        .header("Authorization", bearer(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itens").isEmpty());

    mvc.perform(get("/api/conversas")
        .header("Authorization", bearer(10))
        .param("arquivadas", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.itens[0].ativa").value(false));

    mvc.perform(get("/api/conversas/{id}/mensagens", conversaId)
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));

    mvc.perform(post("/api/conversas/{id}/mensagens", conversaId)
        .header("Authorization", bearer(10))
        .contentType("application/json")
        .content("""
            {"conteudo": "Nova mensagem"}
            """))
        .andExpect(status().isConflict());

    mvc.perform(
        patch("/api/conversas/{id}/arquivamento", conversaId)
            .header("Authorization", bearer(10))
            .contentType("application/json")
            .content("""
                {"arquivada": false}
                """))
        .andExpect(status().isNoContent());

    enviar(10, conversaId, "Agora pode enviar");
  }

  @Test
  void deveIsolarConversasEBloquearTerceirosEmTodasAsOperacoes()
      throws Exception {

    int conversaId = criar(30, 1, 1);
    criar(40, 2, 2);

    for (int usuario : new int[] { 20, 40, 50 }) {
      mvc.perform(get("/api/conversas/{id}/mensagens", conversaId)
          .header("Authorization", bearer(usuario)))
          .andExpect(status().isForbidden());

      mvc.perform(post("/api/conversas/{id}/mensagens", conversaId)
          .header("Authorization", bearer(usuario))
          .contentType("application/json")
          .content("""
              {"conteudo": "Não pode enviar"}
              """))
          .andExpect(status().isForbidden());

      mvc.perform(patch("/api/conversas/{id}/leitura", conversaId)
          .header("Authorization", bearer(usuario)))
          .andExpect(status().isForbidden());

      mvc.perform(
          patch("/api/conversas/{id}/arquivamento", conversaId)
              .header("Authorization", bearer(usuario))
              .contentType("application/json")
              .content("""
                  {"arquivada": true}
                  """))
          .andExpect(status().isForbidden());
    }

    mvc.perform(get("/api/conversas")
        .header("Authorization", bearer(20)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.itens[0].condominioId").value(2))
        .andExpect(jsonPath("$.itens[0].cooperativaId").value(2));

    mvc.perform(get("/api/conversas")
        .header("Authorization", bearer(50)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));

    mvc.perform(post("/api/conversas")
        .header("Authorization", bearer(40))
        .contentType("application/json")
        .content("""
            {"condominioId": 1, "cooperativaId": 1}
            """))
        .andExpect(status().isForbidden());
  }

  @Test
  void deveBloquearParticipanteInativoECondominioInativo()
      throws Exception {

    int conversaId = criar(30, 1, 1);

    jdbc.update("""
        UPDATE tb_usuarios
        SET ativo = FALSE
        WHERE id_usuario = 30
        """);

    mvc.perform(get("/api/conversas/{id}/mensagens", conversaId)
        .header("Authorization", bearer(30)))
        .andExpect(status().isForbidden());

    mvc.perform(post("/api/conversas")
        .header("Authorization", bearer(10))
        .contentType("application/json")
        .content("""
            {"condominioId": 3, "cooperativaId": 1}
            """))
        .andExpect(status().isForbidden());

    mvc.perform(post("/api/conversas")
        .header("Authorization", bearer(40))
        .contentType("application/json")
        .content("""
            {"condominioId": 2, "cooperativaId": 3}
            """))
        .andExpect(status().isForbidden());
  }

  @Test
  void deveValidarConteudoParametrosEAutenticacao()
      throws Exception {

    int conversaId = criar(30, 1, 1);

    mvc.perform(get("/api/conversas"))
        .andExpect(status().isUnauthorized());

    mvc.perform(get("/api/conversas")
        .header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());

    for (String conteudo : new String[] {
        "",
        "   ",
        "x".repeat(2001)
    }) {
      mvc.perform(post("/api/conversas/{id}/mensagens", conversaId)
          .header("Authorization", bearer(10))
          .contentType("application/json")
          .content(json.writeValueAsString(
              Map.of("conteudo", conteudo))))
          .andExpect(status().isBadRequest());
    }

    enviar(10, conversaId, "x".repeat(2000));

    mvc.perform(get("/api/conversas/999/mensagens")
        .header("Authorization", bearer(30)))
        .andExpect(status().isNotFound());

    mvc.perform(get("/api/conversas/abc/mensagens")
        .header("Authorization", bearer(30)))
        .andExpect(status().isBadRequest());

    mvc.perform(get("/api/conversas")
        .header("Authorization", bearer(30))
        .param("size", "101"))
        .andExpect(status().isBadRequest());

    mvc.perform(
        patch("/api/conversas/{id}/arquivamento", conversaId)
            .header("Authorization", bearer(30))
            .contentType("application/json")
            .content("{}"))
        .andExpect(status().isBadRequest());
  }

  private int criar(
      int usuario,
      int condominio,
      int cooperativa) throws Exception {

    var resultado = mvc.perform(post("/api/conversas")
        .header("Authorization", bearer(usuario))
        .contentType("application/json")
        .content(json.writeValueAsString(Map.of(
            "condominioId", condominio,
            "cooperativaId", cooperativa))))
        .andExpect(status().isOk())
        .andReturn();

    return json.readTree(
        resultado.getResponse().getContentAsString())
        .get("id")
        .asInt();
  }

  private long enviar(
      int usuario,
      int conversaId,
      String conteudo) throws Exception {

    var resultado = mvc.perform(
        post("/api/conversas/{id}/mensagens", conversaId)
            .header("Authorization", bearer(usuario))
            .contentType("application/json")
            .content(json.writeValueAsString(
                Map.of("conteudo", conteudo))))
        .andExpect(status().isCreated())
        .andReturn();

    return json.readTree(
        resultado.getResponse().getContentAsString())
        .get("id")
        .asLong();
  }

  private OffsetDateTime leitura(long mensagemId) {
    return jdbc.queryForObject("""
        SELECT lida_em
        FROM tb_mensagens_conversas
        WHERE id_mensagem = ?
        """,
        OffsetDateTime.class,
        mensagemId);
  }

  private String bearer(int usuario) {
    String perfil = switch (usuario) {
      case 30, 40 -> "SINDICO";
      case 50 -> "MORADOR";
      default -> "COOPERATIVA";
    };

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
  @EnableJpaRepositories(basePackageClasses = ConversaRepository.class, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
      ConversaRepository.class,
      MensagemConversaRepository.class
  }))
  @Import({
      SecurityConfig.class,
      JwtService.class,
      ChatService.class,
      ChatGatewayImpl.class,
      ChatPersistenceMapper.class,
      ChatController.class,
      ChatMapper.class,
      ChatExceptionHandler.class
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
          ConversaEntity.class.getName(),
          MensagemConversaEntity.class.getName()));

      // O schema é carregado explicitamente no banco isolado.
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