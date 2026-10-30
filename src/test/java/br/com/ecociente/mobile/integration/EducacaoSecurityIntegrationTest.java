package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import org.springframework.security.core.Authentication;


import br.com.ecociente.mobile.config.security.JwtService;
import br.com.ecociente.mobile.config.security.SecurityConfig;
import br.com.ecociente.mobile.core.service.educacao.AulaService;
import br.com.ecociente.mobile.core.service.educacao.CursoService;
import br.com.ecociente.mobile.dataprovider.entity.educacao.AulaEntity;
import br.com.ecociente.mobile.dataprovider.entity.educacao.CursoEntity;
import br.com.ecociente.mobile.dataprovider.gateway.educacao.AulaGatewayImpl;
import br.com.ecociente.mobile.dataprovider.gateway.educacao.CursoGatewayImpl;
import br.com.ecociente.mobile.dataprovider.mapper.educacao.AulaEntityMapper;
import br.com.ecociente.mobile.dataprovider.mapper.educacao.CursoEntityMapper;
import br.com.ecociente.mobile.dataprovider.repository.educacao.AulaRepository;
import br.com.ecociente.mobile.entrypoint.controller.educacao.AulaController;
import br.com.ecociente.mobile.entrypoint.controller.educacao.CursoController;
import br.com.ecociente.mobile.entrypoint.mapper.educacao.AulaMapper;
import br.com.ecociente.mobile.entrypoint.mapper.educacao.CursoMapper;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Tag("integracao")
@Testcontainers
@SpringBootTest(classes = EducacaoSecurityIntegrationTest.Configuracao.class, properties = {
    "app.security.jwt.secret=segredo-testes-educacao-com-mais-de-32-caracteres",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.sql.init.mode=never",
    "spring.flyway.enabled=false",
    "spring.liquibase.enabled=false",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@Sql("/sql/aulas-test.sql")
class EducacaoSecurityIntegrationTest {

  private static final SecretKey KEY = Keys.hmacShaKeyFor(
      "segredo-testes-educacao-com-mais-de-32-caracteres"
          .getBytes(StandardCharsets.UTF_8));

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
      .withDatabaseName("mobile_educacao_security_test")
      .withUsername("teste")
      .withPassword("teste");

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private String tokenValido() {
    return Jwts.builder()
        .subject("morador@teste.com")
        .claim("usuarioId", 1)
        .claim("nome", "Morador Teste")
        .claim("perfil", "MORADOR")
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plusSeconds(600)))
        .signWith(KEY)
        .compact();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "/api/cursos",
      "/api/cursos/1",
      "/api/cursos/1/aulas",
      "/api/aulas/10"
  })
  void devePermitirConsultaComTokenValido(String caminho)
      throws Exception {

    mvc.perform(get(caminho)
        .header("Authorization", "Bearer " + tokenValido()))
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "/api/cursos",
      "/api/cursos/1",
      "/api/cursos/1/aulas",
      "/api/aulas/10"
  })
  void deveExigirTokenNosEndpoints(String caminho) throws Exception {
    mvc.perform(get(caminho))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "Bearer invalido",
      "Bearer ",
      "Basic abc"
  })
  void deveRejeitarCredenciaisInvalidas(String authorization)
      throws Exception {

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", authorization))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRejeitarTokenExpirado() throws Exception {
    String token = Jwts.builder()
        .subject("morador@teste.com")
        .claim("usuarioId", 1)
        .expiration(Date.from(Instant.now().minusSeconds(60)))
        .signWith(KEY)
        .compact();

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRejeitarTokenAssinadoComOutroSegredo() throws Exception {
    SecretKey outraChave = Keys.hmacShaKeyFor(
        "outro-segredo-testes-com-mais-de-32-caracteres"
            .getBytes(StandardCharsets.UTF_8));

    String token = Jwts.builder()
        .subject("morador@teste.com")
        .claim("usuarioId", 1)
        .expiration(Date.from(Instant.now().plusSeconds(600)))
        .signWith(outraChave)
        .compact();

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRejeitarTokenSemExpiracao() throws Exception {
    String token = Jwts.builder()
        .subject("morador@teste.com")
        .claim("usuarioId", 1)
        .signWith(KEY)
        .compact();

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRejeitarTokenSemUsuarioId() throws Exception {
    String token = Jwts.builder()
        .subject("morador@teste.com")
        .expiration(Date.from(Instant.now().plusSeconds(600)))
        .signWith(KEY)
        .compact();

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -1 })
  void deveRejeitarUsuarioIdNaoPositivo(int usuarioId)
      throws Exception {

    String token = Jwts.builder()
        .subject("morador@teste.com")
        .claim("usuarioId", usuarioId)
        .expiration(Date.from(Instant.now().plusSeconds(600)))
        .signWith(KEY)
        .compact();

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRejeitarUsuarioIdComTipoIncorreto() throws Exception {
    String token = Jwts.builder()
        .subject("morador@teste.com")
        .claim("usuarioId", "abc")
        .expiration(Date.from(Instant.now().plusSeconds(600)))
        .signWith(KEY)
        .compact();

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRejeitarTokenSemSubject() throws Exception {
    String token = Jwts.builder()
        .claim("usuarioId", 1)
        .expiration(Date.from(Instant.now().plusSeconds(600)))
        .signWith(KEY)
        .compact();

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRejeitarTokenComSubjectEmBranco() throws Exception {
    String token = Jwts.builder()
        .subject(" ")
        .claim("usuarioId", 1)
        .expiration(Date.from(Instant.now().plusSeconds(600)))
        .signWith(KEY)
        .compact();

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "/api/cursos/3",
      "/api/cursos/3/aulas",
      "/api/aulas/30"
  })
  void tokenValidoNaoDeveLiberarConteudoDeCursoInativo(String caminho)
      throws Exception {

    mvc.perform(get(caminho)
        .header("Authorization", "Bearer " + tokenValido()))
        .andExpect(status().isNotFound());
  }

  @Test
  void naoDeveReutilizarAutenticacaoNaRequisicaoSeguinte()
      throws Exception {

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", "Bearer " + tokenValido()))
        .andExpect(status().isOk())
        .andExpect(cookie().doesNotExist("JSESSIONID"));

    mvc.perform(get("/api/aulas/10"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejeicaoDoTokenNaoDeveAlterarDados() throws Exception {
    var cursosAntes = jdbc.queryForList(
        "SELECT * FROM tb_cursos ORDER BY id_curso");
    var aulasAntes = jdbc.queryForList(
        "SELECT * FROM tb_aulas ORDER BY id_aula");

    mvc.perform(get("/api/aulas/10")
        .header("Authorization", "Bearer invalido"))
        .andExpect(status().isUnauthorized());

    assertEquals(
        cursosAntes,
        jdbc.queryForList("SELECT * FROM tb_cursos ORDER BY id_curso"));
    assertEquals(
        aulasAntes,
        jdbc.queryForList("SELECT * FROM tb_aulas ORDER BY id_aula"));
  }

  @Test
  void deveDisponibilizarUsuarioIdEPerfilDuranteARequisicao()
      throws Exception {

    mvc.perform(get("/teste/educacao/principal")
        .header("Authorization", "Bearer " + tokenValido()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.usuarioId").value(1))
        .andExpect(jsonPath("$.authorities[0]").value("ROLE_MORADOR"));
  }

  @Test
  void deveProtegerEndpointDeVerificacaoDoPrincipal() throws Exception {
    mvc.perform(get("/teste/educacao/principal"))
        .andExpect(status().isUnauthorized());
  }

  @RestController
public static class PrincipalTesteController {

    @GetMapping("/teste/educacao/principal")
    public Map<String, Object> principal(Authentication authentication) {
        return Map.of(
            "usuarioId", (Integer) authentication.getPrincipal(),
            "authorities", authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList()
        );
    }
}

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EntityScan(basePackageClasses = {
    AulaEntity.class,
    CursoEntity.class
})
@EnableJpaRepositories(basePackageClasses = AulaRepository.class)
@Import({
    EducacaoSecurityIntegrationTest.PrincipalTesteController.class,
    SecurityConfig.class,
    JwtService.class,
    CursoController.class,
    CursoService.class,
    CursoMapper.class,
    CursoGatewayImpl.class,
    CursoEntityMapper.class,
    AulaController.class,
    AulaService.class,
    AulaMapper.class,
    AulaGatewayImpl.class,
    AulaEntityMapper.class
})
static class Configuracao {
 }
}