package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.mobile.core.domain.educacao.Quiz;
import br.com.ecociente.mobile.core.gateway.educacao.QuizGateway;
import br.com.ecociente.mobile.core.service.educacao.QuizService;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizGateway gateway;

    @InjectMocks
    private QuizService service;

    @Test
    void deveRetornarQuizDisponivelPreservandoEscalaDoDominio() {
        Quiz quiz = new Quiz(
                100,
                1,
                10,
                "Quiz de reciclagem",
                new BigDecimal("70.00"),
                30,
                List.of());

        when(gateway.buscarQuizDisponivelPorAulaId(10))
                .thenReturn(Optional.of(quiz));

        assertEquals(Optional.of(quiz), service.buscarQuizDaAula(10));

        verify(gateway).buscarQuizDisponivelPorAulaId(10);
    }

    @Test
    void deveRetornarOptionalVazioParaQuizIndisponivel() {
        when(gateway.buscarQuizDisponivelPorAulaId(11))
                .thenReturn(Optional.empty());

        assertTrue(service.buscarQuizDaAula(11).isEmpty());
    }

    @Test
    void devePropagarFalhaDoGateway() {
        RuntimeException falha = new RuntimeException("Falha de consulta");

        when(gateway.buscarQuizDisponivelPorAulaId(10)).thenThrow(falha);

        assertSame(
                falha,
                assertThrows(
                        RuntimeException.class,
                        () -> service.buscarQuizDaAula(10)));
    }
}