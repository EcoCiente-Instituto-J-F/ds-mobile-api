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
import br.com.ecociente.mobile.core.service.ChatService;
import br.com.ecociente.mobile.dataprovider.gateway.ChatGatewayImpl;
import br.com.ecociente.mobile.entrypoint.controller.ChatController;
import br.com.ecociente.mobile.entrypoint.exception.ChatExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.ChatMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Testcontainers
@SpringJUnitConfig(ChatIntegrationTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
@Execution(ExecutionMode.SAME_THREAD)
class ChatIntegrationTest {

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
      statement.execute(recurso("sql/chat-schema-test.sql"));
    }

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Autowired ChatService service;

  private int criar(int usuario, int condominio, int cooperativa) throws Exception {
    var resultado = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/conversas")
        .header("Authorization", bearer(usuario)).contentType("application/json")
        .content("{\"condominioId\":" + condominio + ",\"cooperativaId\":" + cooperativa + "}"))
        .andExpect(status().isOk()).andReturn();
    return new com.fasterxml.jackson.databind.ObjectMapper().readTree(resultado.getResponse().getContentAsString()).get("id").asInt();
  }
  private long enviar(int usuario, int conversa, String conteudo) throws Exception {
    var resultado = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/conversas/" + conversa + "/mensagens")
        .header("Authorization", bearer(usuario)).contentType("application/json")
        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("conteudo", conteudo))))
        .andExpect(status().isCreated()).andReturn();
    return new com.fasterxml.jackson.databind.ObjectMapper().readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
  }
  @Test
  void deveCriarUmaConversaUnicaMesmoComRequisicoesConcorrentes() throws Exception {
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var primeiro = pool.submit(() -> service.criar(30, 1, 1).id());
      var segundo = pool.submit(() -> service.criar(10, 1, 1).id());
      assertEquals(primeiro.get(10, java.util.concurrent.TimeUnit.SECONDS), segundo.get(10, java.util.concurrent.TimeUnit.SECONDS));
    }
    assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM tb_conversas", Long.class));
  }
  @Test
  void deveDerivarRemetenteDoTokenAtualizarResumoELerSomenteRecebidasAteOLimite() throws Exception {
    int id = criar(30, 1, 1);
    long propria = enviar(30, id, "Olá cooperativa");
    long recebida = enviar(10, id, "Olá síndico");
    long posterior = enviar(10, id, "Mais uma mensagem");
    assertEquals("CONDOMINIO", jdbc.queryForObject("SELECT remetente_tipo FROM tb_mensagens_conversas WHERE id_mensagem = ?", String.class, propria));
    mvc.perform(get("/api/conversas").header("Authorization", bearer(30)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].naoLidas").value(2))
        .andExpect(jsonPath("$.itens[0].ultimaMensagemEm").exists());
    for (int repeticao = 0; repeticao < 2; repeticao++) {
      mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/conversas/" + id + "/leitura")
          .header("Authorization", bearer(30)).contentType("application/json")
          .content("{\"ateMensagemId\":" + recebida + "}"))
          .andExpect(status().isOk()).andExpect(jsonPath("$.mensagensMarcadas").value(repeticao == 0 ? 1 : 0));
    }
    assertNull(jdbc.queryForObject("SELECT lida_em FROM tb_mensagens_conversas WHERE id_mensagem = ?", java.time.OffsetDateTime.class, propria));
    assertNotNull(jdbc.queryForObject("SELECT lida_em FROM tb_mensagens_conversas WHERE id_mensagem = ?", java.time.OffsetDateTime.class, recebida));
    assertNull(jdbc.queryForObject("SELECT lida_em FROM tb_mensagens_conversas WHERE id_mensagem = ?", java.time.OffsetDateTime.class, posterior));
    mvc.perform(get("/api/conversas/" + id + "/mensagens").header("Authorization", bearer(10)).param("size", "2"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.itens[0].id").value(posterior)).andExpect(jsonPath("$.totalPaginas").value(2));
    mvc.perform(get("/api/conversas/" + id + "/mensagens").header("Authorization", bearer(10)).param("page", "99"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.itens").isEmpty());
  }
  @Test
  void deveArquivarParaAmbosPreservarHistoricoEDesarquivar() throws Exception {
    int id = criar(30, 1, 1);
    enviar(10, id, "Histórico");
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/conversas/" + id + "/arquivamento")
        .header("Authorization", bearer(30)).contentType("application/json").content("{\"arquivada\":true}"))
        .andExpect(status().isNoContent());
    assertEquals(id, criar(10, 1, 1));
    mvc.perform(get("/api/conversas").header("Authorization", bearer(10)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.itens").isEmpty());
    mvc.perform(get("/api/conversas").header("Authorization", bearer(10)).param("arquivadas", "true"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    mvc.perform(get("/api/conversas/" + id + "/mensagens").header("Authorization", bearer(30)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/conversas/" + id + "/mensagens")
        .header("Authorization", bearer(10)).contentType("application/json").content("{\"conteudo\":\"Nova\"}"))
        .andExpect(status().isConflict());
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/conversas/" + id + "/arquivamento")
        .header("Authorization", bearer(10)).contentType("application/json").content("{\"arquivada\":false}"))
        .andExpect(status().isNoContent());
    enviar(10, id, "Nova");
  }
  @Test
  void deveIsolarParticipantesEmTodasAsOperacoes() throws Exception {
    int id = criar(30, 1, 1);
    criar(60, 3, 2);
    for (int usuario : new int[]{20, 40, 60, 70}) {
      mvc.perform(get("/api/conversas/" + id + "/mensagens").header("Authorization", bearer(usuario)))
          .andExpect(status().isForbidden());
      mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/conversas/" + id + "/mensagens")
          .header("Authorization", bearer(usuario)).contentType("application/json").content("{\"conteudo\":\"Teste\"}"))
          .andExpect(status().isForbidden());
      mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/conversas/" + id + "/leitura")
          .header("Authorization", bearer(usuario))).andExpect(status().isForbidden());
      mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/conversas/" + id + "/arquivamento")
          .header("Authorization", bearer(usuario)).contentType("application/json").content("{\"arquivada\":true}"))
          .andExpect(status().isForbidden());
    }
    mvc.perform(get("/api/conversas").header("Authorization", bearer(20)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.itens[0].condominioId").value(3));
    mvc.perform(get("/api/conversas").header("Authorization", bearer(70)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/conversas")
        .header("Authorization", bearer(60)).contentType("application/json").content("{\"condominioId\":1,\"cooperativaId\":1}"))
        .andExpect(status().isForbidden());
    jdbc.update("UPDATE tb_usuarios SET ativo=FALSE WHERE id_usuario=30");
    mvc.perform(get("/api/conversas/" + id + "/mensagens").header("Authorization", bearer(30)))
        .andExpect(status().isForbidden());
  }
  @Test
  void deveValidarConteudoTokenParametrosEConversaInexistente() throws Exception {
    int id = criar(30, 1, 1);
    mvc.perform(get("/api/conversas")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/conversas").header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
    for (String conteudo : new String[]{"", "   ", "x".repeat(2001)}) {
      mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/conversas/" + id + "/mensagens")
          .header("Authorization", bearer(10)).contentType("application/json")
          .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("conteudo", conteudo))))
          .andExpect(status().isBadRequest());
    }
    enviar(10, id, "x".repeat(2000));
    mvc.perform(get("/api/conversas/999/mensagens").header("Authorization", bearer(30)))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/conversas/abc/mensagens").header("Authorization", bearer(30)))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/conversas").header("Authorization", bearer(30)).param("size", "101"))
        .andExpect(status().isBadRequest());
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
      ChatService.class,
      ChatGatewayImpl.class,
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
    PlatformTransactionManager transactionManager(
        DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }
  }
}