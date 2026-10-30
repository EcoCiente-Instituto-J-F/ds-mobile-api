package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.ecociente.mobile.core.domain.educacao.ResumoTentativaQuiz;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException.Tipo;
import br.com.ecociente.mobile.core.exception.educacao.FluxoQuizException;
import br.com.ecociente.mobile.core.gateway.educacao.TentativaQuizGateway;
import br.com.ecociente.mobile.core.service.educacao.ConsultarRevisaoQuizService;


@ExtendWith(MockitoExtension.class)
class ConsultarRevisaoQuizServiceTest {

  @Mock
  private TentativaQuizGateway gateway;

  @InjectMocks
  private ConsultarRevisaoQuizService service;

  private static final ResumoTentativaQuiz RESUMO = new ResumoTentativaQuiz(
      1, 100, "Quiz Um", new BigDecimal("80"), true, OffsetDateTime.parse("2026-10-30T12:00:00Z"));

  @Test
  void historicoDeveUsarOUsuarioEOLimiteInformados() {
    when(gateway.buscarTipoUsuarioAtivo(10)).thenReturn(Optional.of("Usuário comum"));
    when(gateway.listarConcluidas(10, 20)).thenReturn(List.of(RESUMO));

    assertEquals(List.of(RESUMO), service.historico(10, 20));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 50})
  void historicoDeveAceitarOsLimitesDoIntervalo(int limite) {
    when(gateway.buscarTipoUsuarioAtivo(10)).thenReturn(Optional.of("Usuário comum"));
    when(gateway.listarConcluidas(10, limite)).thenReturn(List.of());

    assertEquals(List.of(), service.historico(10, limite));
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 51})
  void historicoDeveRejeitarLimiteForaDoIntervalo(int limite) {
    when(gateway.buscarTipoUsuarioAtivo(10)).thenReturn(Optional.of("Usuário comum"));

    var erro = assertThrows(FluxoQuizException.class, () -> service.historico(10, limite));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("LIMITE_INVALIDO", erro.getCodigo());
    verify(gateway, never()).listarConcluidas(anyInt(), anyInt());
  }

  @Test
  void historicoDeveRejeitarUsuarioInativo() {
    when(gateway.buscarTipoUsuarioAtivo(10)).thenReturn(Optional.empty());

    var erro = assertThrows(FluxoQuizException.class, () -> service.historico(10, 20));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    assertEquals("USUARIO_INVALIDO", erro.getCodigo());
    verify(gateway, never()).listarConcluidas(anyInt(), anyInt());
  }

  @Test
  void historicoDeveRejeitarUsuarioNuloOuInvalido() {
    assertEquals("USUARIO_INVALIDO",
        assertThrows(FluxoQuizException.class, () -> service.historico(null, 20)).getCodigo());
    assertEquals("USUARIO_INVALIDO",
        assertThrows(FluxoQuizException.class, () -> service.historico(0, 20)).getCodigo());
  }

  @Test
  void quizzesConcluidosDeveRetornarSoOsDoUsuario() {
    when(gateway.buscarTipoUsuarioAtivo(10)).thenReturn(Optional.of("Usuário comum"));
    when(gateway.listarQuizzesConcluidos(10)).thenReturn(List.of(12, 18, 25));

    assertEquals(List.of(12, 18, 25), service.quizzesConcluidos(10));
    verify(gateway, never()).listarQuizzesConcluidos(30);
  }

  @Test
  void quizzesConcluidosDeveRejeitarUsuarioInativo() {
    when(gateway.buscarTipoUsuarioAtivo(10)).thenReturn(Optional.empty());

    var erro = assertThrows(FluxoQuizException.class, () -> service.quizzesConcluidos(10));

    assertEquals("USUARIO_INVALIDO", erro.getCodigo());
    verify(gateway, never()).listarQuizzesConcluidos(anyInt());
  }
}
