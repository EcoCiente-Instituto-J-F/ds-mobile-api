package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.ecociente.mobile.core.domain.*;
import br.com.ecociente.mobile.core.exception.FluxoQuizException;
import br.com.ecociente.mobile.core.gateway.QuizGateway;
import br.com.ecociente.mobile.core.gateway.TentativaQuizGateway;

class FinalizarTentativaQuizServiceTest {

    private QuizGateway quizzes;
    private TentativaQuizGateway tentativas;
    private FinalizarTentativaQuizService service;

    @BeforeEach
    void configurar() {
        quizzes = mock(QuizGateway.class);
        tentativas = mock(TentativaQuizGateway.class);

        service = new FinalizarTentativaQuizService(
                quizzes, tentativas);

        when(tentativas.buscarTipoUsuarioAtivo(10))
                .thenReturn(Optional.of("Usuário comum"));

        when(tentativas.buscarComBloqueio(1000))
                .thenReturn(Optional.of(aberta()));

        when(quizzes.buscarQuizDisponivelPorId(100))
                .thenReturn(Optional.of(quiz("70")));

        when(tentativas.buscarGabarito(100)).thenReturn(List.of(
                new AlternativaCorrecaoQuiz(1, 10, true),
                new AlternativaCorrecaoQuiz(1, 11, false),
                new AlternativaCorrecaoQuiz(2, 20, true),
                new AlternativaCorrecaoQuiz(2, 21, false)));

        when(tentativas.concluir(
                anyInt(), any(BigDecimal.class),
                anyBoolean(), any(), any())).thenAnswer(
                        invocacao -> new TentativaQuizEstado(
                                invocacao.getArgument(0),
                                10,
                                100,
                                invocacao.getArgument(1),
                                invocacao.getArgument(2),
                                OffsetDateTime.now()));
    }

    @Test
    void deveCorrigirESalvarAprovacao() {
        var resultado = service.executar(
                10, 1000, corretas());

        assertEquals(new BigDecimal("100.00"), resultado.nota());
        assertEquals(2, resultado.acertos());
        assertEquals(2, resultado.totalPerguntas());
        assertTrue(resultado.aprovado());

        verify(tentativas).salvarRespostas(
                eq(1000),
                argThat(respostas -> respostas.size() == 2
                        && respostas.contains(
                                new RespostaQuizRegistrada(1, 10, true))
                        && respostas.contains(
                                new RespostaQuizRegistrada(2, 20, true))));
    }

    @Test
    void deveReprovarQuandoNotaForInsuficiente() {
        var resultado = service.executar(
                10,
                1000,
                List.of(
                        new RespostaQuiz(1, 10),
                        new RespostaQuiz(2, 21)));

        assertEquals(new BigDecimal("50.00"), resultado.nota());
        assertEquals(1, resultado.acertos());
        assertFalse(resultado.aprovado());
    }

    @Test
    void deveAprovarNaNotaMinimaExata() {
        when(quizzes.buscarQuizDisponivelPorId(100))
                .thenReturn(Optional.of(quiz("50")));

        var resultado = service.executar(
                10,
                1000,
                List.of(
                        new RespostaQuiz(1, 10),
                        new RespostaQuiz(2, 21)));

        assertTrue(resultado.aprovado());
    }

    @Test
    void deveSalvarContextoDoMorador() {
        when(tentativas.buscarTipoUsuarioAtivo(10))
                .thenReturn(Optional.of("Morador"));

        when(tentativas.buscarContextosAprovadosDoMorador(10))
                .thenReturn(List.of(
                        new ContextoCondominialQuiz(5, 8)));

        service.executar(10, 1000, corretas());

        verify(tentativas).concluir(
                1000,
                new BigDecimal("100.00"),
                true,
                5,
                8);
    }

    @Test
    void moradorResidencialEUsuarioComercialPodemFinalizar() {
        for (String tipo : List.of("Morador Residencial", "Usuário Comercial")) {
            when(tentativas.buscarTipoUsuarioAtivo(10))
                    .thenReturn(Optional.of(tipo));

            assertDoesNotThrow(() -> service.executar(10, 1000, corretas()));
        }
    }

    @Test
    void sindicoCooperativaEAdminNaoPodemFinalizar() {
        for (String tipo : List.of("Síndico Residencial", "Cooperativa", "Administrador", "admin")) {
            when(tentativas.buscarTipoUsuarioAtivo(10))
                    .thenReturn(Optional.of(tipo));

            var erro = assertThrows(FluxoQuizException.class,
                    () -> service.executar(10, 1000, corretas()));

            assertEquals("QUIZ_PERFIL_NAO_PERMITIDO", erro.getCodigo());
        }
    }

    @Test
    void usuarioComumNaoDeveReceberContextoCondominial() {
        service.executar(10, 1000, corretas());

        verify(tentativas).concluir(
                1000,
                new BigDecimal("100.00"),
                true,
                null,
                null);

        verify(tentativas, never())
                .buscarContextosAprovadosDoMorador(anyInt());
    }

    @Test
    void naoDeveFinalizarTentativaDeOutroUsuario() {
        when(tentativas.buscarComBloqueio(1000))
                .thenReturn(Optional.of(new TentativaQuizEstado(
                        1000, 20, 100, null, false, null)));

        assertThrows(
                FluxoQuizException.class,
                () -> service.executar(10, 1000, corretas()));

        verificarQueNaoSalvou();
    }

    @Test
    void naoDeveAceitarPerguntaRepetida() {
        assertThrows(
                FluxoQuizException.class,
                () -> service.executar(
                        10, 1000,
                        List.of(
                                new RespostaQuiz(1, 10),
                                new RespostaQuiz(1, 11))));

        verificarQueNaoSalvou();
    }

    @Test
    void naoDeveAceitarRespostasIncompletas() {
        assertThrows(
                FluxoQuizException.class,
                () -> service.executar(
                        10, 1000,
                        List.of(new RespostaQuiz(1, 10))));

        verificarQueNaoSalvou();
    }

    @Test
    void naoDeveAceitarAlternativaDeOutraPergunta() {
        assertThrows(
                FluxoQuizException.class,
                () -> service.executar(
                        10, 1000,
                        List.of(
                                new RespostaQuiz(1, 20),
                                new RespostaQuiz(2, 21))));

        verificarQueNaoSalvou();
    }

    @Test
    void naoDeveAceitarPerguntaDeOutroQuiz() {
        assertThrows(
                FluxoQuizException.class,
                () -> service.executar(
                        10, 1000,
                        List.of(
                                new RespostaQuiz(1, 10),
                                new RespostaQuiz(999, 20))));

        verificarQueNaoSalvou();
    }

    @Test
    void naoDeveAceitarDuasAlternativasCorretas() {
        when(tentativas.buscarGabarito(100)).thenReturn(List.of(
                new AlternativaCorrecaoQuiz(1, 10, true),
                new AlternativaCorrecaoQuiz(1, 11, true),
                new AlternativaCorrecaoQuiz(2, 20, true)));

        assertThrows(
                FluxoQuizException.class,
                () -> service.executar(10, 1000, corretas()));

        verificarQueNaoSalvou();
    }

    @Test
    void deveRecuperarFinalizacaoSemGravarNovamente() {
        when(tentativas.buscarComBloqueio(1000))
                .thenReturn(Optional.of(new TentativaQuizEstado(
                        1000, 10, 100,
                        new BigDecimal("100.00"),
                        true,
                        OffsetDateTime.now())));

        when(tentativas.buscarRespostas(1000)).thenReturn(List.of(
                new RespostaQuizRegistrada(1, 10, true),
                new RespostaQuizRegistrada(2, 20, true)));

        var resultado = service.executar(
                10, 1000, corretas());

        assertEquals(2, resultado.acertos());
        verificarQueNaoSalvou();
        verifyNoInteractions(quizzes);
    }

    @Test
    void naoDeveAlterarRespostasDepoisDaConclusao() {
        when(tentativas.buscarComBloqueio(1000))
                .thenReturn(Optional.of(new TentativaQuizEstado(
                        1000, 10, 100,
                        new BigDecimal("100.00"),
                        true,
                        OffsetDateTime.now())));

        when(tentativas.buscarRespostas(1000)).thenReturn(List.of(
                new RespostaQuizRegistrada(1, 10, true),
                new RespostaQuizRegistrada(2, 20, true)));

        assertThrows(
                FluxoQuizException.class,
                () -> service.executar(
                        10, 1000,
                        List.of(
                                new RespostaQuiz(1, 11),
                                new RespostaQuiz(2, 21))));

        verificarQueNaoSalvou();
    }

    private void verificarQueNaoSalvou() {
        verify(tentativas, never())
                .salvarRespostas(anyInt(), anyList());

        verify(tentativas, never()).concluir(
                anyInt(), any(), anyBoolean(), any(), any());
    }

    private TentativaQuizEstado aberta() {
        return new TentativaQuizEstado(
                1000, 10, 100, null, false, null);
    }

    private List<RespostaQuiz> corretas() {
        return List.of(
                new RespostaQuiz(1, 10),
                new RespostaQuiz(2, 20));
    }

    private Quiz quiz(String notaMinima) {
        return new Quiz(
                100, 1, 10,
                "Quiz",
                new BigDecimal(notaMinima),
                30,
                List.of());
    }
}