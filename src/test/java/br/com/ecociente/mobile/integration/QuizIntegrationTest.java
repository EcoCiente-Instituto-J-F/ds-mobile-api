package br.com.ecociente.mobile.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ecociente.mobile.config.security.JwtService;
import br.com.ecociente.mobile.config.security.SecurityConfig;
import br.com.ecociente.mobile.core.service.educacao.QuizService;
import br.com.ecociente.mobile.dataprovider.entity.QuizEntity;
import br.com.ecociente.mobile.dataprovider.gateway.educacao.QuizGatewayImpl;
import br.com.ecociente.mobile.dataprovider.mapper.educacao.QuizEntityMapper;
import br.com.ecociente.mobile.dataprovider.repository.QuizRepository;
import br.com.ecociente.mobile.entrypoint.controller.educacao.QuizController;
import br.com.ecociente.mobile.entrypoint.mapper.educacao.QuizMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Tag("integracao")
@Testcontainers
@SpringBootTest(
    classes = QuizIntegrationTest.Configuracao.class,
    properties = {
        "app.security.jwt.secret=segredo-testes-quiz-com-mais-de-32-caracteres",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false",
        "spring.jpa.open-in-view=false"
    }
)
@AutoConfigureMockMvc
@Sql("/sql/quizzes-test.sql")
class QuizIntegrationTest {

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
        "segredo-testes-quiz-com-mais-de-32-caracteres"
            .getBytes(StandardCharsets.UTF_8)
    );

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mobile_quiz_test")
            .withUsername("teste")
            .withPassword("teste");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token() {
        return Jwts.builder()
            .subject("morador@teste.com")
            .claim("usuarioId", 1)
            .claim("perfil", "MORADOR")
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(KEY)
            .compact();
    }

    @Test
    void deveRetornarQuizPerguntasOrdenadasEAlternativasSemGabarito()
        throws Exception {

        var resultado = mvc.perform(get("/api/aulas/10/quiz")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.APPLICATION_JSON
            ))
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.cursoId").value(1))
            .andExpect(jsonPath("$.aulaId").value(10))
            .andExpect(jsonPath("$.titulo").value("Quiz de reciclagem"))
            .andExpect(jsonPath("$.notaMinimaAprovacao").value(7.0))
            .andExpect(jsonPath("$.pontosRecompensa").value(30))
            .andExpect(jsonPath("$.perguntas.length()").value(2))
            .andExpect(jsonPath("$.perguntas[0].id").value(1001))
            .andExpect(jsonPath("$.perguntas[0].ordem").value(1))
            .andExpect(jsonPath("$.perguntas[1].id").value(1000))
            .andExpect(jsonPath("$.perguntas[1].ordem").value(2))
            .andExpect(jsonPath(
                "$.perguntas[0].alternativas.length()"
            ).value(2))
            .andExpect(jsonPath(
                "$.perguntas[0].alternativas[0].id"
            ).value(5001))
            .andExpect(jsonPath(
                "$.perguntas[0].alternativas[0].texto"
            ).value("Facilitar a reciclagem."))
            .andExpect(jsonPath(
                "$.perguntas[0].alternativas[1].id"
            ).value(5002))
            .andExpect(jsonPath(
                "$.perguntas[1].alternativas[0].id"
            ).value(5003))
            .andExpect(jsonPath(
                "$.perguntas[1].alternativas[1].id"
            ).value(5004))
            .andReturn();

        JsonNode resposta = objectMapper.readTree(
            resultado.getResponse().getContentAsString(
                StandardCharsets.UTF_8
            )
        );

        assertTrue(
            resposta.findValues("correta").isEmpty(),
            "O response não pode expor o gabarito"
        );
    }

    @Test
    void deveIsolarPerguntasEAlternativasEntreQuizzes() throws Exception {
        mvc.perform(get("/api/aulas/20/quiz")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(200))
            .andExpect(jsonPath("$.notaMinimaAprovacao").value(8.0))
            .andExpect(jsonPath("$.perguntas.length()").value(1))
            .andExpect(jsonPath("$.perguntas[0].id").value(2000))
            .andExpect(jsonPath(
                "$.perguntas[0].alternativas.length()"
            ).value(2))
            .andExpect(jsonPath(
                "$.perguntas[0].alternativas[0].id"
            ).value(6001))
            .andExpect(jsonPath(
                "$.perguntas[0].alternativas[1].id"
            ).value(6002));
    }

    @ParameterizedTest
    @ValueSource(ints = {11, 12, 13, 30, 999, 0, -1})
    void deveRetornar404ParaQuizIndisponivel(int aulaId)
        throws Exception {

        mvc.perform(get("/api/aulas/{aulaId}/quiz", aulaId)
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "2147483648"})
    void deveRejeitarIdQueNaoPodeSerConvertidoParaInteger(String aulaId)
        throws Exception {

        mvc.perform(get("/api/aulas/{aulaId}/quiz", aulaId)
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveExigirToken() throws Exception {
        mvc.perform(get("/api/aulas/10/quiz"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarTokenInvalido() throws Exception {
        mvc.perform(get("/api/aulas/10/quiz")
                .header("Authorization", "Bearer invalido"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRefletirDesativacaoDoQuiz() throws Exception {
        jdbc.update(
            "UPDATE tb_quizzes SET ativo = FALSE WHERE id_quiz = ?",
            100
        );

        mvc.perform(get("/api/aulas/10/quiz")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveRefletirDesativacaoDoCurso() throws Exception {
        jdbc.update(
            "UPDATE tb_cursos SET esta_ativo = FALSE WHERE id_curso = ?",
            1
        );

        mvc.perform(get("/api/aulas/10/quiz")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isNotFound());

        mvc.perform(get("/api/aulas/20/quiz")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "70.00", "75.50", "100.00"})
    void deveConverterNotaMinimaSemModificarValorNoBanco(String valor)
        throws Exception {

        BigDecimal notaBanco = new BigDecimal(valor);

        jdbc.update(
            """
            UPDATE tb_quizzes
            SET nota_minima_aprovacao = ?
            WHERE id_quiz = ?
            """,
            notaBanco,
            100
        );

        var resultado = mvc.perform(get("/api/aulas/10/quiz")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode resposta = objectMapper.readTree(
            resultado.getResponse().getContentAsString()
        );

        BigDecimal notaMobile = new BigDecimal(
            resposta.get("notaMinimaAprovacao").asText()
        );

        assertEquals(
            0,
            notaBanco.movePointLeft(1).compareTo(notaMobile)
        );

        BigDecimal persistida = jdbc.queryForObject(
            """
            SELECT nota_minima_aprovacao
            FROM tb_quizzes
            WHERE id_quiz = 100
            """,
            BigDecimal.class
        );

        assertEquals(0, notaBanco.compareTo(persistida));
    }

    @Test
    void deveRetornarListaVaziaParaQuizSemPerguntas() throws Exception {
        mvc.perform(get("/api/aulas/14/quiz")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perguntas").isEmpty());
    }

    @Test
    void devePermitirConsultaDePerguntaSemAlternativas() throws Exception {
        jdbc.update(
            "DELETE FROM tb_alternativas_quiz WHERE pergunta_id = ?",
            1001
        );

        mvc.perform(get("/api/aulas/10/quiz")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perguntas[0].id").value(1001))
            .andExpect(jsonPath(
                "$.perguntas[0].alternativas"
            ).isEmpty());
    }

    @Test
    void consultaNaoDeveAlterarQuizPerguntasOuAlternativas()
        throws Exception {

        var quizzesAntes = jdbc.queryForList(
            "SELECT * FROM tb_quizzes ORDER BY id_quiz"
        );
        var perguntasAntes = jdbc.queryForList(
            "SELECT * FROM tb_perguntas_quiz ORDER BY id_pergunta"
        );
        var alternativasAntes = jdbc.queryForList(
            "SELECT * FROM tb_alternativas_quiz ORDER BY id_alternativa"
        );

        mvc.perform(get("/api/aulas/10/quiz")
                .header("Authorization", "Bearer " + token()))
            .andExpect(status().isOk());

        assertEquals(
            quizzesAntes,
            jdbc.queryForList("SELECT * FROM tb_quizzes ORDER BY id_quiz")
        );
        assertEquals(
            perguntasAntes,
            jdbc.queryForList(
                "SELECT * FROM tb_perguntas_quiz ORDER BY id_pergunta"
            )
        );
        assertEquals(
            alternativasAntes,
            jdbc.queryForList(
                "SELECT * FROM tb_alternativas_quiz ORDER BY id_alternativa"
            )
        );
    }

    @Test
    void bancoDeveImpedirSegundoQuizParaMesmaAula() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbc.update("""
                INSERT INTO tb_quizzes (
                    curso_id, aula_id, titulo_quiz, pontos_recompensa
                )
                VALUES (1, 10, 'Outro quiz', 30)
                """)
        );
    }

    @Test
    void bancoDeveImpedirPerguntasComOrdemDuplicadaNoQuiz() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbc.update("""
                INSERT INTO tb_perguntas_quiz (quiz_id, enunciado, ordem)
                VALUES (100, 'Pergunta duplicada', 1)
                """)
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = QuizEntity.class)
    @EnableJpaRepositories(basePackageClasses = QuizRepository.class)
    @Import({
        SecurityConfig.class,
        JwtService.class,
        QuizController.class,
        QuizService.class,
        QuizMapper.class,
        QuizGatewayImpl.class,
        QuizEntityMapper.class
    })
    static class Configuracao {
    }
}