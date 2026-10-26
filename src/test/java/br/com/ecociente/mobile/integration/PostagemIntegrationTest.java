package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

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
import br.com.ecociente.mobile.core.service.ConsultarCategoriasResiduoService;
import br.com.ecociente.mobile.core.service.ConsultarPostagensCondominioService;
import br.com.ecociente.mobile.core.service.ConsultarPostagensUsuarioService;
import br.com.ecociente.mobile.core.domain.ResultadoVoto;
import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.gateway.ValidadorPostagemGateway;
import br.com.ecociente.mobile.core.service.CriarPostagemService;
import br.com.ecociente.mobile.core.service.VotarPostagemService;
import br.com.ecociente.mobile.dataprovider.gateway.PostagemGatewayImpl;
import br.com.ecociente.mobile.entrypoint.controller.CategoriaResiduoController;
import br.com.ecociente.mobile.entrypoint.controller.PostagemController;
import br.com.ecociente.mobile.entrypoint.exception.PostagemExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.PostagemMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Testcontainers
@SpringJUnitConfig(PostagemIntegrationTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
    "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
@Execution(ExecutionMode.SAME_THREAD)
class PostagemIntegrationTest {

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
    Config.USUARIO_RECEBIDO.set(null);
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      statement.execute("DROP SCHEMA public CASCADE");
      statement.execute("CREATE SCHEMA public");
      statement.execute(recurso("sql/ecociente-schema.sql"));
      statement.execute(recurso("sql/postagens-dados-test.sql"));
    }

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  private static String corpo(String urlFoto, String hash, int categoria, int condominio, OffsetDateTime captura) {
    return """
        {"urlFoto":"%s","hashFoto":"%s","capturadaEm":"%s","categoriaId":%d,"condominioId":%d}
        """.formatted(urlFoto, hash, captura, categoria, condominio);
  }

  private static OffsetDateTime haCincoMinutos() {
    return OffsetDateTime.now().minusMinutes(5);
  }

  @Test
  void deveCriarPostagemEmValidacaoComATorreDoMorador() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo("https://img.teste/nova.jpg", "hash-nova", 1, 1, haCincoMinutos())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.usuarioId").value(20))
        .andExpect(jsonPath("$.autor").value("Ana Morador"))
        .andExpect(jsonPath("$.condominioId").value(1))
        .andExpect(jsonPath("$.torreId").value(1))
        .andExpect(jsonPath("$.categoria").value("Plástico"))
        .andExpect(jsonPath("$.statusValidacao").value("em_analise"))
        .andExpect(jsonPath("$.saldoConfianca").value(0))
        .andExpect(jsonPath("$.resolvidoEm").doesNotExist());

    assertEquals(
        1L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM tb_postagens WHERE hash_foto = 'hash-nova' AND torre_id = 1",
            Long.class));
  }

  @Test
  void devePermitirMoradorSemTorreComTorreNula() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo("https://img.teste/b2.jpg", "hash-b2", 2, 1, haCincoMinutos())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.torreId").doesNotExist());
  }

  @Test
  void deveRejeitarFotoRepetida() throws Exception {
    String json = corpo("https://img.teste/nova.jpg", "hash-nova", 1, 1, haCincoMinutos());

    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isCreated());

    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(30))
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("FOTO_JA_UTILIZADA"));
  }

  @Test
  void deveRejeitarHashJaExistenteNoBanco() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo("https://img.teste/x.jpg", "hash-bia", 1, 1, haCincoMinutos())))
        .andExpect(status().isConflict());
  }

  @Test
  void naoDevePostarSemVinculoAprovado() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(50))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo("https://img.teste/d.jpg", "hash-d", 1, 1, haCincoMinutos())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("SEM_VINCULO_APROVADO"));
  }

  @Test
  void naoDevePostarEmCondominioDeOutro() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo("https://img.teste/e.jpg", "hash-e", 1, 2, haCincoMinutos())))
        .andExpect(status().isForbidden());
  }

  @Test
  void deveInformarCategoriaInexistente() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo("https://img.teste/f.jpg", "hash-f", 99, 1, haCincoMinutos())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("CATEGORIA_NAO_ENCONTRADA"));
  }

  @Test
  void deveListarAsCategoriasDeResiduo() throws Exception {
    mvc.perform(get("/api/categorias-residuos")
        .header("Authorization", bearer(20)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].nome").value("Papel"))
        .andExpect(jsonPath("$[0].id").value(2))
        .andExpect(jsonPath("$[0].pontosBase").value(10))
        .andExpect(jsonPath("$[1].nome").value("Plástico"));

    mvc.perform(get("/api/categorias-residuos"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRejeitarMoradorComTorreDeOutroCondominio() throws Exception {
    jdbc.update("UPDATE tb_moradores SET torre_id = 2 WHERE usuario_id = 20");

    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo("https://img.teste/t.jpg", "hash-t", 1, 1, haCincoMinutos())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("TORRE_CONDOMINIO_INCONSISTENTE"));
  }

  @Test
  void deveRejeitarCapturaNoFuturo() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo("https://img.teste/g.jpg", "hash-g", 1, 1, OffsetDateTime.now().plusHours(1))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("CAPTURA_INVALIDA"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{}",
      "{nao e json",
      "{\"urlFoto\":\"\"}",
      "[]"
  })
  void deveRejeitarCorpoInvalido(String corpo) throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deveRejeitarCorpoSemCamposObrigatorios() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ERRO_VALIDACAO"));
  }

  @Test
  void deveExigirJwt() throws Exception {
    mvc.perform(get("/api/postagens/minhas"))
        .andExpect(status().isUnauthorized());

    mvc.perform(post("/api/postagens")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveListarFeedSoDoCondominioDoMaisNovoAoMaisAntigo() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo("https://img.teste/nova.jpg", "hash-nova", 1, 1, haCincoMinutos())))
        .andExpect(status().isCreated());

    mvc.perform(get("/api/postagens")
        .header("Authorization", bearer(20))
        .param("condominioId", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.pagina").value(0))
        .andExpect(jsonPath("$.tamanho").value(10))
        .andExpect(jsonPath("$.totalPaginas").value(1))
        .andExpect(jsonPath("$.postagens.length()").value(2))
        .andExpect(jsonPath("$.postagens[0].autor").value("Ana Morador"))
        .andExpect(jsonPath("$.postagens[1].id").value(1))
        .andExpect(jsonPath("$.postagens[1].statusValidacao").value("em_analise"));
  }

  @Test
  void naoDeveListarFeedDeOutroCondominio() throws Exception {
    mvc.perform(get("/api/postagens")
        .header("Authorization", bearer(20))
        .param("condominioId", "2"))
        .andExpect(status().isForbidden());
  }

  @Test
  void devePaginarOFeed() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo("https://img.teste/nova.jpg", "hash-nova", 1, 1, haCincoMinutos())))
        .andExpect(status().isCreated());

    mvc.perform(get("/api/postagens")
        .header("Authorization", bearer(20))
        .param("condominioId", "1")
        .param("size", "1")
        .param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.totalPaginas").value(2))
        .andExpect(jsonPath("$.postagens.length()").value(1))
        .andExpect(jsonPath("$.postagens[0].id").value(1));
  }

  @Test
  void deveListarSoAsPostagensDoProprioUsuario() throws Exception {
    mvc.perform(get("/api/postagens/minhas")
        .header("Authorization", bearer(30)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.postagens.length()").value(1))
        .andExpect(jsonPath("$.postagens[0].id").value(1));

    mvc.perform(get("/api/postagens/minhas")
        .header("Authorization", bearer(20)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.totalPaginas").value(0))
        .andExpect(jsonPath("$.postagens").isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "-1", "101", "abc" })
  void deveRejeitarTamanhoInvalido(String tamanho) throws Exception {
    mvc.perform(get("/api/postagens/minhas")
        .header("Authorization", bearer(20))
        .param("size", tamanho))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deveRejeitarPaginaNegativa() throws Exception {
    mvc.perform(get("/api/postagens/minhas")
        .header("Authorization", bearer(20))
        .param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("PAGINACAO_INVALIDA"));
  }

  @Test
  void deveRejeitarCondominioAusenteOuInvalido() throws Exception {
    mvc.perform(get("/api/postagens")
        .header("Authorization", bearer(20)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));

    mvc.perform(get("/api/postagens")
        .header("Authorization", bearer(20))
        .param("condominioId", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
  }

  @Test
  void deveRejeitarJsonInvalido() throws Exception {
    mvc.perform(post("/api/postagens")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{nao e json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));
  }

  @Test
  void deveRegistrarVotoDoMorador() throws Exception {
    mvc.perform(post("/api/postagens/1/votos")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"tipo\":\"aprovar\"}"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"saldoConfianca\":3,\"pontuacaoAtiva\":true}", true));
  }

  @Test
  void deveUsarUsuarioDoJwtEIgnorarUsuarioIdDoCorpo() throws Exception {
    mvc.perform(post("/api/postagens/1/votos")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"tipo\":\"aprovar\",\"usuarioId\":99}"))
        .andExpect(status().isOk());

    assertEquals(20, Config.USUARIO_RECEBIDO.get());
  }

  @Test
  void deveRejeitarTipoDeVotoInvalido() throws Exception {
    mvc.perform(post("/api/postagens/1/votos")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"tipo\":\"curtir\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("ERRO_VALIDACAO"));
  }

  @Test
  void deveDevolverConflitoQuandoJaVotou() throws Exception {
    mvc.perform(post("/api/postagens/999/votos")
        .header("Authorization", bearer(20))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"tipo\":\"aprovar\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("VOTO_JA_REGISTRADO"));
  }

  @Test
  void deveExigirJwtParaVotar() throws Exception {
    mvc.perform(post("/api/postagens/1/votos")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"tipo\":\"aprovar\"}"))
        .andExpect(status().isUnauthorized());
  }

  private String bearer(int usuario) {
    String token = Jwts.builder()
        .subject("usuario-" + usuario + "@teste.com")
        .claim("usuarioId", usuario)
        .claim("perfil", "MORADOR")
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plusSeconds(600)))
        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
        .compact();

    return "Bearer " + token;
  }

  private String recurso(String caminho) throws Exception {
    try (var stream = new ClassPathResource(caminho).getInputStream()) {
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Configuration
  @EnableWebMvc
  @EnableTransactionManagement
  @Import({
      SecurityConfig.class,
      JwtService.class,
      CriarPostagemService.class,
      ConsultarPostagensCondominioService.class,
      ConsultarPostagensUsuarioService.class,
      ConsultarCategoriasResiduoService.class,
      VotarPostagemService.class,
      PostagemGatewayImpl.class,
      PostagemMapper.class,
      PostagemController.class,
      CategoriaResiduoController.class,
      PostagemExceptionHandler.class
  })
  static class Config {

    static final AtomicReference<Integer> USUARIO_RECEBIDO = new AtomicReference<>();

    @Bean
    ValidadorPostagemGateway validadorPostagemGateway() {
      return (postagemId, usuarioId, tipo, motivoDenunciaId, comentario) -> {
        USUARIO_RECEBIDO.set(usuarioId);
        if (postagemId == 999) {
          throw new PostagemException(
              PostagemException.Tipo.CONFLITO,
              "VOTO_JA_REGISTRADO",
              "Você já votou nesta postagem");
        }
        return new ResultadoVoto(3, true);
      };
    }

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
    PlatformTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }
  }
}
