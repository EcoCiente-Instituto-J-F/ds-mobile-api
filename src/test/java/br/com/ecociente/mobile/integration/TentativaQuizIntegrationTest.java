package br.com.ecociente.mobile.integration;

import br.com.ecociente.mobile.dataprovider.gateway.educacao.QuizGatewayImpl;
import br.com.ecociente.mobile.dataprovider.gateway.educacao.TentativaQuizGatewayImpl;
import br.com.ecociente.mobile.dataprovider.gateway.educacao.PontuacaoQuizGatewayImpl;
import br.com.ecociente.mobile.core.service.educacao.IniciarTentativaQuizService;
import br.com.ecociente.mobile.core.service.educacao.FinalizarTentativaQuizService;
import br.com.ecociente.mobile.core.service.educacao.FinalizarQuizComPontuacaoService;
import br.com.ecociente.mobile.core.service.educacao.ConsultarRevisaoQuizService;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ecociente.mobile.config.security.JwtService;
import br.com.ecociente.mobile.config.security.SecurityConfig;

import br.com.ecociente.mobile.dataprovider.mapper.educacao.QuizEntityMapper;
import br.com.ecociente.mobile.entrypoint.controller.educacao.TentativaQuizController;
import br.com.ecociente.mobile.entrypoint.exception.educacao.FluxoQuizExceptionHandler;
import br.com.ecociente.mobile.entrypoint.mapper.educacao.QuizMapper;
import br.com.ecociente.mobile.entrypoint.mapper.educacao.TentativaQuizMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Testcontainers
@AutoConfigureMockMvc
@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest(classes = TentativaQuizIntegrationTest.Config.class, properties = {
        "spring.mail.host=localhost",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.open-in-view=false",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false",
        "app.security.jwt.secret=0123456789abcdef0123456789abcdef"
})
class TentativaQuizIntegrationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.com.ecociente.mobile.config.security.UsuarioDetailsService usuarioDetailsServiceTeste;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.com.ecociente.mobile.core.service.pontuacao.ConsolidarQuizInternoService consolidacaoInternaSimulada;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private br.com.ecociente.mobile.core.service.pontuacao.SincronizacaoPontuacaoService sincronizacaoRedisSimulada;

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private static final String CORRETAS = """
            {
              "respostas":[
                {"perguntaId":1,"alternativaId":10},
                {"perguntaId":2,"alternativaId":20}
              ]
            }
            """;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final AtomicInteger statusPontuacao = new AtomicInteger(200);

    private static final AtomicInteger usuarioPontuacao = new AtomicInteger(10);

    private static final AtomicInteger chamadas = new AtomicInteger();

    private static final AtomicBoolean viuCommit = new AtomicBoolean();

    private static final AtomicReference<JdbcTemplate> banco = new AtomicReference<>();

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DataSource dataSource;

    @Autowired
    ObjectMapper json;

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void preparar() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            statement.execute("DROP SCHEMA public CASCADE");
            statement.execute("CREATE SCHEMA public");

            statement.execute(recurso(
                    "sql/ecociente-schema.sql"));

            statement.execute(recurso(
                    "sql/tentativas-quiz-dados-test.sql"));
        }

        statusPontuacao.set(200);
        usuarioPontuacao.set(10);
        chamadas.set(0);
        viuCommit.set(false);
        banco.set(jdbc);

        org.mockito.Mockito.when(
                consolidacaoInternaSimulada.executar(
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(invocacao -> {
                    Integer tentativaId = invocacao.getArgument(0);
                    Integer usuarioId = invocacao.getArgument(1);

                    chamadas.incrementAndGet();

                    assertFalse(
                            org.springframework.transaction.support.TransactionSynchronizationManager
                                    .isActualTransactionActive(),
                            "A consolidação deve ser chamada após o commit da tentativa");

                    Boolean concluida = jdbc.queryForObject(
                            """
                                    SELECT concluido_em IS NOT NULL
                                    FROM tb_tentativas_quiz
                                    WHERE id_tentativa = ?
                                    """,
                            Boolean.class,
                            tentativaId);

                    viuCommit.set(Boolean.TRUE.equals(concluida));

                    if (statusPontuacao.get() != 200) {
                        throw new org.springframework.dao.DataAccessResourceFailureException(
                                "Falha simulada na consolidação");
                    }

                    return br.com.ecociente.mobile.core.domain.pontuacao.MovimentacaoPontos
                            .builder()
                            .id(999L)
                            .usuarioId(usuarioId)
                            .tentativaQuizId(tentativaId)
                            .origemTipo(
                                    br.com.ecociente.mobile.core.domain.pontuacao.OrigemPontuacaoType.QUIZ)
                            .tipoMovimentacao(
                                    br.com.ecociente.mobile.core.domain.pontuacao.TipoMovimentacaoType.CREDITO)
                            .pontos(30)
                            .redisSincronizado(false)
                            .build();
                });

    }

    @Test
    void deveIniciarSemExporGabarito() throws Exception {
        var resposta = mvc.perform(
                post("/api/quizzes/100/tentativas")
                        .header("Authorization", bearer(10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quiz.notaMinimaAprovacao")
                        .value(7))
                .andExpect(jsonPath("$.quiz.perguntas.length()")
                        .value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(resposta.contains("\"correta\""));
        assertEquals(1L, contarTentativas());
        assertEquals(0, chamadas.get());
    }

    @Test
    void deveCorrigirCommitarEConsolidar() throws Exception {
        int id = iniciar(10);

        finalizar(id, 10, CORRETAS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(10))
                .andExpect(jsonPath("$.acertos").value(2))
                .andExpect(jsonPath("$.aprovado").value(true))
                .andExpect(jsonPath("$.pontuacao.status")
                        .value("CONFIRMADO"))
                .andExpect(jsonPath("$.pontuacao.pontos").value(30));

        assertTrue(
                viuCommit.get(),
                "A pontuação deve enxergar a tentativa já concluída");
        assertEquals(2L, contarRespostas(id));

        assertEquals(
                0,
                jdbc.queryForObject("""
                        SELECT nota
                        FROM tb_tentativas_quiz
                        WHERE id_tentativa = ?
                        """, java.math.BigDecimal.class, id)
                        .compareTo(new java.math.BigDecimal("100")));
    }

    @Test
    void reprovadoNaoDeveChamarPontuacao() throws Exception {
        int id = iniciar(10);

        finalizar(id, 10, """
                {
                  "respostas":[
                    {"perguntaId":1,"alternativaId":10},
                    {"perguntaId":2,"alternativaId":21}
                  ]
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(5))
                .andExpect(jsonPath("$.aprovado").value(false))
                .andExpect(jsonPath("$.pontuacao.status")
                        .value("NAO_APLICAVEL"));

        assertEquals(0, chamadas.get());
    }

    @Test
    void deveSalvarCondominioETorreDoMorador() throws Exception {
        usuarioPontuacao.set(20);
        int id = iniciar(20);

        finalizar(id, 20, CORRETAS)
                .andExpect(status().isOk());

        var contexto = jdbc.queryForMap("""
                SELECT condominio_id, torre_id
                FROM tb_tentativas_quiz
                WHERE id_tentativa = ?
                """, id);

        assertEquals(1, contexto.get("condominio_id"));
        assertEquals(1, contexto.get("torre_id"));
    }

    @Test
    void naoDeveUsarTorreDeOutroCondominio() {
        var erro = assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("""
                        UPDATE tb_moradores
                        SET torre_id = 2
                        WHERE usuario_id = 20
                        """));

        assertTrue(
                erro.getMostSpecificCause().getMessage()
                        .contains("fk_moradores_torre_condominio"));

        Integer torreAtual = jdbc.queryForObject("""
                SELECT torre_id
                FROM tb_moradores
                WHERE usuario_id = 20
                """, Integer.class);

        assertNotEquals(Integer.valueOf(2), torreAtual);
    }

    @Test
    void devePreservarResultadoEPermitirRetryDaPontuacao()
            throws Exception {

        int id = iniciar(10);
        statusPontuacao.set(503);

        finalizar(id, 10, CORRETAS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pontuacao.status")
                        .value("PENDENTE"));

        assertEquals(2L, contarRespostas(id));

        statusPontuacao.set(200);

        finalizar(id, 10, CORRETAS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pontuacao.status")
                        .value("CONFIRMADO"));

        assertEquals(2L, contarRespostas(id));
        assertEquals(2, chamadas.get());
    }

    @Test
    void naoDeveTrocarRespostasConcluidas() throws Exception {
        int id = iniciar(10);

        finalizar(id, 10, CORRETAS)
                .andExpect(status().isOk());

        finalizar(id, 10, """
                {
                  "respostas":[
                    {"perguntaId":1,"alternativaId":11},
                    {"perguntaId":2,"alternativaId":21}
                  ]
                }
                """)
                .andExpect(status().isConflict());

        assertEquals(2L, contarRespostas(id));
        assertEquals(1, chamadas.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"respostas\":[]}",
            "{\"respostas\":[null]}",
            """
                    {"respostas":[
                      {"perguntaId":1,"alternativaId":10}
                    ]}
                    """,
            """
                    {"respostas":[
                      {"perguntaId":1,"alternativaId":10},
                      {"perguntaId":1,"alternativaId":11}
                    ]}
                    """,
            """
                    {"respostas":[
                      {"perguntaId":1,"alternativaId":20},
                      {"perguntaId":2,"alternativaId":21}
                    ]}
                    """
    })
    void deveRejeitarRespostasInvalidasSemGravar(String corpo)
            throws Exception {

        int id = iniciar(10);

        finalizar(id, 10, corpo)
                .andExpect(status().isBadRequest());

        assertEquals(0L, contarRespostas(id));
        assertEquals(0, chamadas.get());
        assertNaoConcluida(id);
    }

    @Test
    void somenteDonoDeveFinalizarERevisar() throws Exception {
        int id = iniciar(10);

        finalizar(id, 30, CORRETAS)
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/tentativas-quiz/{id}/revisao", id)
                .header("Authorization", bearer(30)))
                .andExpect(status().isForbidden());

        assertEquals(0L, contarRespostas(id));
    }

    @Test
    void deveBloquearRevisaoAntesDaConclusao() throws Exception {
        int id = iniciar(10);

        mvc.perform(get("/api/tentativas-quiz/{id}/revisao", id)
                .header("Authorization", bearer(10)))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornarRevisaoOrdenada() throws Exception {
        int id = iniciar(10);

        finalizar(id, 10, CORRETAS)
                .andExpect(status().isOk());

        int chamadasAntes = chamadas.get();

        mvc.perform(get("/api/tentativas-quiz/{id}/revisao", id)
                .header("Authorization", bearer(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(10))
                .andExpect(jsonPath("$.acertos").value(2))
                .andExpect(jsonPath("$.gabaritoAtual").value(true))
                .andExpect(jsonPath("$.perguntas[0].perguntaId")
                        .value(1))
                .andExpect(jsonPath(
                        "$.perguntas[0].respostaEscolhida.id").value(10))
                .andExpect(jsonPath(
                        "$.perguntas[0].respostaCorreta.id").value(10))
                .andExpect(jsonPath(
                        "$.perguntas[0].acertouNaTentativa").value(true))
                .andExpect(jsonPath("$.perguntas[1].perguntaId")
                        .value(2));

        assertEquals(chamadasAntes, chamadas.get());
    }

    @Test
    void deveNegarQuizInativoEPerfilNaoPermitido()
            throws Exception {

        mvc.perform(post("/api/quizzes/100/tentativas")
                .header("Authorization", bearer(40)))
                .andExpect(status().isForbidden());

        jdbc.update("""
                UPDATE tb_quizzes SET ativo = FALSE
                WHERE id_quiz = 100
                """);

        mvc.perform(post("/api/quizzes/100/tentativas")
                .header("Authorization", bearer(10)))
                .andExpect(status().isNotFound());

        assertEquals(0L, contarTentativas());
    }

    @Test
    void deveExigirJwtNosTresEndpoints() throws Exception {
        mvc.perform(post("/api/quizzes/100/tentativas"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/tentativas-quiz/1/finalizar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CORRETAS))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/tentativas-quiz/1/revisao"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveExigirJwtNasConsultasDeHistorico() throws Exception {
        mvc.perform(get("/api/tentativas-quiz"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/tentativas-quiz/quizzes-concluidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarUsuarioInativoNasConsultasDeHistorico() throws Exception {
        jdbc.update("UPDATE tb_usuarios SET ativo = FALSE WHERE id_usuario = 10");

        historico(10, "").andExpect(status().isForbidden());
        historico(10, "/quizzes-concluidos").andExpect(status().isForbidden());
    }

    @Test
    void historicoVazioDeveRetornarListaVazia() throws Exception {
        historico(10, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        historico(10, "/quizzes-concluidos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void historicoDeveTrazerReprovadoEmOrdemDecrescente() throws Exception {
        int primeira = iniciar(10);
        finalizar(primeira, 10, CORRETAS).andExpect(status().isOk());

        int segunda = iniciar(10);
        finalizar(segunda, 10, """
                {
                  "respostas":[
                    {"perguntaId":1,"alternativaId":10},
                    {"perguntaId":2,"alternativaId":21}
                  ]
                }
                """).andExpect(status().isOk());

        historico(10, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tentativaId").value(segunda))
                .andExpect(jsonPath("$[0].quizId").value(100))
                .andExpect(jsonPath("$[0].titulo").value("Quiz Um"))
                .andExpect(jsonPath("$[0].nota").value(50))
                .andExpect(jsonPath("$[0].aprovado").value(false))
                .andExpect(jsonPath("$[0].concluidoEm").isNotEmpty())
                .andExpect(jsonPath("$[1].tentativaId").value(primeira))
                .andExpect(jsonPath("$[1].nota").value(100))
                .andExpect(jsonPath("$[1].aprovado").value(true));
    }

    @Test
    void historicoNaoDeveIncluirTentativaEmAndamento() throws Exception {
        iniciar(10);

        historico(10, "").andExpect(jsonPath("$.length()").value(0));
        historico(10, "/quizzes-concluidos").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void historicoDeveRespeitarOLimiteERejeitarValoresInvalidos() throws Exception {
        finalizar(iniciar(10), 10, CORRETAS).andExpect(status().isOk());
        finalizar(iniciar(10), 10, CORRETAS).andExpect(status().isOk());

        historico(10, "?limite=1").andExpect(jsonPath("$.length()").value(1));

        for (String invalido : new String[] { "-1", "0", "51" }) {
            String corpo = historico(10, "?limite=" + invalido)
                    .andExpect(status().isBadRequest())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertTrue(corpo.contains("LIMITE_INVALIDO"), corpo);
        }
    }

    @Test
    void historicoNaoDeveExporTentativasDeOutroUsuario() throws Exception {
        finalizar(iniciar(10), 10, CORRETAS).andExpect(status().isOk());

        historico(30, "").andExpect(jsonPath("$.length()").value(0));
        historico(30, "/quizzes-concluidos").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void quizzesConcluidosDeveListarCadaQuizUmaVezIncluindoReprovado() throws Exception {
        finalizar(iniciar(10), 10, CORRETAS).andExpect(status().isOk());
        finalizar(iniciar(10), 10, """
                {
                  "respostas":[
                    {"perguntaId":1,"alternativaId":10},
                    {"perguntaId":2,"alternativaId":21}
                  ]
                }
                """).andExpect(status().isOk());

        historico(10, "/quizzes-concluidos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value(100));
    }

    private org.springframework.test.web.servlet.ResultActions historico(
            int usuario,
            String consulta) throws Exception {

        return mvc.perform(
                get("/api/tentativas-quiz" + consulta)
                        .header("Authorization", bearer(usuario)));
    }

    private int iniciar(int usuario) throws Exception {
        String resposta = mvc.perform(
                post("/api/quizzes/100/tentativas")
                        .header("Authorization", bearer(usuario)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return json.readTree(resposta)
                .get("tentativaId").asInt();
    }

    private org.springframework.test.web.servlet.ResultActions finalizar(
            int id,
            int usuario,
            String corpo) throws Exception {

        return mvc.perform(
                post("/api/tentativas-quiz/{id}/finalizar", id)
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo));
    }

    private long contarTentativas() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM tb_tentativas_quiz",
                Long.class);
    }

    private long contarRespostas(int id) {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM tb_rel_respostas_tentativas_quiz
                WHERE tentativa_id = ?
                """, Long.class, id);
    }

    private void assertNaoConcluida(int id) {
        assertNull(jdbc.queryForObject("""
                SELECT concluido_em
                FROM tb_tentativas_quiz
                WHERE id_tentativa = ?
                """,
                (rs, rowNum) -> rs.getObject("concluido_em"),
                id));
    }

    private String bearer(int usuario) {
        String perfil = usuario == 20
                ? "MORADOR"
                : usuario == 40 ? "SINDICO" : "COMUM";

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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("br.com.ecociente.mobile.dataprovider.entity")
    @EnableJpaRepositories("br.com.ecociente.mobile.dataprovider.repository")
    @Import({
            SecurityConfig.class,
            JwtService.class,
            QuizGatewayImpl.class,
            QuizEntityMapper.class,
            TentativaQuizGatewayImpl.class,
            PontuacaoQuizGatewayImpl.class,
            IniciarTentativaQuizService.class,
            FinalizarTentativaQuizService.class,
            FinalizarQuizComPontuacaoService.class,
            ConsultarRevisaoQuizService.class,
            TentativaQuizController.class,
            QuizMapper.class,
            TentativaQuizMapper.class,
            FluxoQuizExceptionHandler.class
    })
    static class Config {
    }
}