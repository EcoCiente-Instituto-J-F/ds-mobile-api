package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DuplicateKeyException;

import br.com.ecociente.mobile.core.domain.AgendamentoAvaliavel;
import br.com.ecociente.mobile.core.domain.AvaliacaoColeta;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException.Tipo;
import br.com.ecociente.mobile.core.gateway.AvaliacaoColetaGateway;

class AvaliarColetaServiceTest {

  private AvaliacaoColetaGateway gateway;
  private AvaliarColetaService service;

  @BeforeEach
  void configurar() {
    gateway = mock(AvaliacaoColetaGateway.class);
    service = new AvaliarColetaService(gateway);
  }

  private void agendamentoRealizadoDoSindico(int sindico) {
    when(gateway.buscarAgendamento(100))
        .thenReturn(Optional.of(new AgendamentoAvaliavel(100, sindico, true)));
    when(gateway.obterOuCriarVisitaRealizada(100)).thenReturn(1000);
    when(gateway.visitaJaAvaliada(1000)).thenReturn(false);
  }

  @Test
  void deveRegistrarAvaliacaoDoSindicoDoCondominio() {
    agendamentoRealizadoDoSindico(30);

    when(gateway.registrar(100, 1000, 30, 5, "Pontual"))
        .thenReturn(new AvaliacaoColeta(
            7, 100, 1000, 5, "Pontual", OffsetDateTime.now()));

    var resultado = service.executar(30, 100, 5, "  Pontual  ");

    assertEquals(7, resultado.id());
    assertEquals(5, resultado.nota());
    assertEquals(1000, resultado.visitaId());

    verify(gateway).registrar(100, 1000, 30, 5, "Pontual");
  }

  @Test
  void comentarioEmBrancoDeveVirarNulo() {
    agendamentoRealizadoDoSindico(30);

    when(gateway.registrar(100, 1000, 30, 4, null))
        .thenReturn(new AvaliacaoColeta(
            7, 100, 1000, 4, null, OffsetDateTime.now()));

    service.executar(30, 100, 4, "   ");

    verify(gateway).registrar(100, 1000, 30, 4, null);
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -1, 6, 100 })
  void deveRejeitarNotaForaDoIntervalo(int nota) {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, nota, null));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("NOTA_INVALIDA", erro.getCodigo());

    verifyNoInteractions(gateway);
  }

  @Test
  void deveRejeitarNotaAusente() {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, null, null));

    assertEquals("NOTA_INVALIDA", erro.getCodigo());
  }

  @Test
  void deveRejeitarComentarioMaiorQueOLimite() {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, "a".repeat(501)));

    assertEquals("COMENTARIO_INVALIDO", erro.getCodigo());

    verifyNoInteractions(gateway);
  }

  @Test
  void deveInformarAgendamentoNaoEncontrado() {
    when(gateway.buscarAgendamento(100)).thenReturn(Optional.empty());

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals(Tipo.NAO_ENCONTRADO, erro.getTipo());
    assertEquals("AGENDAMENTO_NAO_ENCONTRADO", erro.getCodigo());
  }

  @Test
  void naoDeveAvaliarColetaDeOutroSindico() {
    when(gateway.buscarAgendamento(100))
        .thenReturn(Optional.of(new AgendamentoAvaliavel(100, 31, true)));

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    assertEquals("AVALIACAO_NAO_PERMITIDA", erro.getCodigo());

    verify(gateway, never()).registrar(
        anyInt(), anyInt(), anyInt(), anyInt(), any());
  }

  @Test
  void naoDeveAvaliarCondominioSemSindico() {
    when(gateway.buscarAgendamento(100))
        .thenReturn(Optional.of(new AgendamentoAvaliavel(100, null, true)));

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
  }

  @Test
  void naoDeveAvaliarColetaQueNaoFoiRealizada() {
    when(gateway.buscarAgendamento(100))
        .thenReturn(Optional.of(new AgendamentoAvaliavel(100, 30, false)));

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals(Tipo.CONFLITO, erro.getTipo());
    assertEquals("COLETA_NAO_REALIZADA", erro.getCodigo());

    verify(gateway, never()).obterOuCriarVisitaRealizada(anyInt());
  }

  @Test
  void naoDeveAvaliarDuasVezes() {
    agendamentoRealizadoDoSindico(30);
    when(gateway.visitaJaAvaliada(1000)).thenReturn(true);

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals(Tipo.CONFLITO, erro.getTipo());
    assertEquals("AVALIACAO_JA_REGISTRADA", erro.getCodigo());

    verify(gateway, never()).registrar(
        anyInt(), anyInt(), anyInt(), anyInt(), any());
  }

  @Test
  void devePerceberAvaliacaoDuplicadaPorCorrida() {
    agendamentoRealizadoDoSindico(30);
    when(gateway.registrar(100, 1000, 30, 5, null))
        .thenThrow(new DuplicateKeyException("uq"));

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals("AVALIACAO_JA_REGISTRADA", erro.getCodigo());
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -5 })
  void deveRejeitarUsuarioInvalido(int usuario) {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(usuario, 100, 5, null));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -1 })
  void deveRejeitarAgendamentoInvalido(int agendamento) {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, agendamento, 5, null));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("AGENDAMENTO_ID_INVALIDO", erro.getCodigo());
  }
}
