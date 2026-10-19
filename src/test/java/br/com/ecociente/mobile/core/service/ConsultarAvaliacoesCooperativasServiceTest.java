package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.ecociente.mobile.core.domain.ResumoAvaliacoesCooperativa;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException.Tipo;
import br.com.ecociente.mobile.core.gateway.AvaliacaoCooperativaGateway;

class ConsultarAvaliacoesCooperativaServiceTest {

  private AvaliacaoCooperativaGateway gateway;
  private ConsultarAvaliacoesCooperativaService service;

  @BeforeEach
  void configurar() {
    gateway = mock(AvaliacaoCooperativaGateway.class);
    service = new ConsultarAvaliacoesCooperativaService(gateway);
  }

  @Test
  void deveConsultarCooperativaDoResponsavel() {
    when(gateway.buscarResponsavelAtivo(1))
        .thenReturn(Optional.of(10));

    when(gateway.consultarResumo(1))
        .thenReturn(new ResumoAvaliacoesCooperativa(
            23, new BigDecimal("4.25")));

    when(gateway.listar(1, 10L, 10))
        .thenReturn(List.of());

    var resultado = service.executar(10, 1, 1, 10);

    assertEquals(1, resultado.cooperativaId());
    assertEquals(23L, resultado.total());
    assertEquals(new BigDecimal("4.25"), resultado.media());
    assertEquals(1, resultado.pagina());
    assertEquals(10, resultado.tamanho());
    assertEquals(3L, resultado.totalPaginas());

    verify(gateway).listar(1, 10L, 10);
  }

  @Test
  void naoDeveConsultarAvaliacoesDeOutroResponsavel() {
    when(gateway.buscarResponsavelAtivo(1))
        .thenReturn(Optional.of(20));

    var erro = assertThrows(
        ConsultaCooperativaException.class,
        () -> service.executar(10, 1, 0, 10));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());

    verify(gateway, never()).consultarResumo(anyInt());
    verify(gateway, never()).listar(
        anyInt(), anyLong(), anyInt());
  }

  @Test
  void deveInformarCooperativaNaoEncontrada() {
    when(gateway.buscarResponsavelAtivo(1))
        .thenReturn(Optional.empty());

    var erro = assertThrows(
        ConsultaCooperativaException.class,
        () -> service.executar(10, 1, 0, 10));

    assertEquals(Tipo.NAO_ENCONTRADO, erro.getTipo());

    verify(gateway, never()).consultarResumo(anyInt());
  }

  @ParameterizedTest
  @CsvSource({
      "-1,10",
      "0,0",
      "0,-1",
      "0,101"
  })
  void deveRejeitarPaginacaoInvalida(
      int pagina,
      int tamanho) {
    var erro = assertThrows(
        ConsultaCooperativaException.class,
        () -> service.executar(10, 1, pagina, tamanho));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    verifyNoInteractions(gateway);
  }

  @Test
  void deveRejeitarUsuarioAusente() {
    var erro = assertThrows(
        ConsultaCooperativaException.class,
        () -> service.executar(null, 1, 0, 10));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    verifyNoInteractions(gateway);
  }

  @Test
  void deveRejeitarIdDaCooperativaInvalido() {
    var erro = assertThrows(
        ConsultaCooperativaException.class,
        () -> service.executar(10, 0, 0, 10));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    verifyNoInteractions(gateway);
  }

  @Test
  void deveRetornarMediaNulaQuandoNaoExistiremAvaliacoes() {
    when(gateway.buscarResponsavelAtivo(1))
        .thenReturn(Optional.of(10));

    when(gateway.consultarResumo(1))
        .thenReturn(new ResumoAvaliacoesCooperativa(0, null));

    when(gateway.listar(1, 0L, 10))
        .thenReturn(List.of());

    var resultado = service.executar(10, 1, 0, 10);

    assertEquals(0L, resultado.total());
    assertEquals(0L, resultado.totalPaginas());
    assertNull(resultado.media());
    assertTrue(resultado.avaliacoes().isEmpty());
  }
}