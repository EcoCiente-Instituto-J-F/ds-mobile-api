package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.ecociente.mobile.core.domain.AgendamentoAvaliavel;
import br.com.ecociente.mobile.core.domain.AvaliacaoColeta;
import br.com.ecociente.mobile.core.domain.NovaAvaliacaoColeta;
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

  private void agendamento(Integer sindico, boolean realizado) {
    when(gateway.buscarAgendamento(100))
        .thenReturn(Optional.of(new AgendamentoAvaliavel(100, sindico, realizado)));
  }

  private AvaliacaoColeta salva() {
    return new AvaliacaoColeta(7, 100, 1000, 5, "Pontual", OffsetDateTime.now());
  }

  @Test
  void deveAvaliarUsandoAVisitaJaRegistrada() {
    agendamento(30, true);
    when(gateway.buscarVisitaRealizada(100)).thenReturn(Optional.of(1000));
    when(gateway.visitaJaAvaliada(1000)).thenReturn(false);
    when(gateway.registrar(eq(1000), any())).thenReturn(salva());

    var resultado = service.executar(30, 100, 5, "  Pontual  ");

    assertEquals(7, resultado.id());
    assertEquals(1000, resultado.visitaId());

    verify(gateway, never()).criarVisitaRealizada(anyInt());
    verify(gateway).registrar(
        1000,
        new NovaAvaliacaoColeta(30, 100, 5, "Pontual"));
  }

  @Test
  void deveCriarAVisitaQuandoNinguemRegistrou() {
    agendamento(30, true);
    when(gateway.buscarVisitaRealizada(100)).thenReturn(Optional.empty());
    when(gateway.criarVisitaRealizada(100)).thenReturn(1001);
    when(gateway.visitaJaAvaliada(1001)).thenReturn(false);
    when(gateway.registrar(eq(1001), any())).thenReturn(salva());

    service.executar(30, 100, 5, null);

    verify(gateway).criarVisitaRealizada(100);
    verify(gateway).registrar(1001, new NovaAvaliacaoColeta(30, 100, 5, null));
  }

  @Test
  void naoDeveConsultarOBancoQuandoOPedidoEInvalido() {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 9, null));

    assertEquals("NOTA_INVALIDA", erro.getCodigo());

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
    agendamento(31, true);

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    assertEquals("AVALIACAO_NAO_PERMITIDA", erro.getCodigo());

    verify(gateway, never()).registrar(anyInt(), any());
  }

  @Test
  void naoDeveAvaliarCondominioSemSindico() {
    agendamento(null, true);

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
  }

  @Test
  void naoDeveAvaliarColetaQueNaoFoiRealizada() {
    agendamento(30, false);

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals(Tipo.CONFLITO, erro.getTipo());
    assertEquals("COLETA_NAO_REALIZADA", erro.getCodigo());

    verify(gateway, never()).buscarVisitaRealizada(anyInt());
    verify(gateway, never()).criarVisitaRealizada(anyInt());
  }

  @Test
  void naoDeveAvaliarDuasVezes() {
    agendamento(30, true);
    when(gateway.buscarVisitaRealizada(100)).thenReturn(Optional.of(1000));
    when(gateway.visitaJaAvaliada(1000)).thenReturn(true);

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals(Tipo.CONFLITO, erro.getTipo());
    assertEquals("AVALIACAO_JA_REGISTRADA", erro.getCodigo());

    verify(gateway, never()).registrar(anyInt(), any());
  }

  @Test
  void deveRepassarOConflitoDeAvaliacaoSimultanea() {
    agendamento(30, true);
    when(gateway.buscarVisitaRealizada(100)).thenReturn(Optional.of(1000));
    when(gateway.visitaJaAvaliada(1000)).thenReturn(false);
    when(gateway.registrar(eq(1000), any()))
        .thenThrow(AvaliacaoColetaException.jaAvaliada());

    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> service.executar(30, 100, 5, null));

    assertEquals("AVALIACAO_JA_REGISTRADA", erro.getCodigo());
  }
}
