package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.ecociente.mobile.core.exception.PostagemException;
import br.com.ecociente.mobile.core.exception.PostagemException.Tipo;
import br.com.ecociente.mobile.core.gateway.PostagemAnaliseGateway;

class ConsultarPostagensEmAnaliseServiceTest {

  private PostagemAnaliseGateway gateway;
  private ConsultarPostagensEmAnaliseService service;

  @BeforeEach
  void configurar() {
    gateway = mock(PostagemAnaliseGateway.class);
    service = new ConsultarPostagensEmAnaliseService(gateway);
  }

  @Test
  void deveConsultarPostagensDoSindicoResponsavel() {
    when(gateway.buscarResponsavelAtivo(1))
        .thenReturn(Optional.of(30));

    when(gateway.contarEmAnalise(1))
        .thenReturn(23L);

    when(gateway.listarEmAnalise(1, 10L, 10))
        .thenReturn(List.of());

    var resultado = service.executar(30, 1, 1, 10);

    assertEquals(23L, resultado.total());
    assertEquals(1, resultado.pagina());
    assertEquals(10, resultado.tamanho());
    assertEquals(3L, resultado.totalPaginas());

    verify(gateway).listarEmAnalise(1, 10L, 10);
  }

  @Test
  void naoDeveConsultarPostagensDeOutroSindico() {
    when(gateway.buscarResponsavelAtivo(1))
        .thenReturn(Optional.of(40));

    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(30, 1, 0, 10));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());

    verify(gateway, never()).contarEmAnalise(anyInt());

    verify(gateway, never()).listarEmAnalise(
        anyInt(),
        anyLong(),
        anyInt());
  }

  @Test
  void deveInformarCondominioIndisponivel() {
    when(gateway.buscarResponsavelAtivo(1))
        .thenReturn(Optional.empty());

    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(30, 1, 0, 10));

    assertEquals(Tipo.NAO_ENCONTRADO, erro.getTipo());

    verify(gateway, never()).contarEmAnalise(anyInt());
  }

  @Test
  void deveRetornarPaginaVaziaQuandoNaoHaPostagens() {
    when(gateway.buscarResponsavelAtivo(1))
        .thenReturn(Optional.of(30));

    when(gateway.contarEmAnalise(1))
        .thenReturn(0L);

    when(gateway.listarEmAnalise(1, 0L, 10))
        .thenReturn(List.of());

    var resultado = service.executar(30, 1, 0, 10);

    assertEquals(0L, resultado.total());
    assertEquals(0L, resultado.totalPaginas());
    assertTrue(resultado.postagens().isEmpty());
  }

  @Test
  void devePreservarTotalAoConsultarPaginaAlemDoFinal() {
    when(gateway.buscarResponsavelAtivo(1))
        .thenReturn(Optional.of(30));

    when(gateway.contarEmAnalise(1))
        .thenReturn(3L);

    when(gateway.listarEmAnalise(1, 198L, 2))
        .thenReturn(List.of());

    var resultado = service.executar(30, 1, 99, 2);

    assertEquals(3L, resultado.total());
    assertEquals(2L, resultado.totalPaginas());
    assertTrue(resultado.postagens().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
      "0,0,10",
      "-1,0,10",
      "1,-1,10",
      "1,0,0",
      "1,0,-1",
      "1,0,101"
  })
  void deveRejeitarParametrosInvalidos(
      int condominioId,
      int pagina,
      int tamanho) {

    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(
            30,
            condominioId,
            pagina,
            tamanho));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    verifyNoInteractions(gateway);
  }

  @Test
  void deveRejeitarUsuarioAusente() {
    var erro = assertThrows(
        PostagemException.class,
        () -> service.executar(null, 1, 0, 10));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    verifyNoInteractions(gateway);
  }

  @Test
  void deveCalcularDeslocamentoSemOverflowDeInteiro() {
    when(gateway.buscarResponsavelAtivo(1))
        .thenReturn(Optional.of(30));

    when(gateway.listarEmAnalise(1, 214748364700L, 100))
        .thenReturn(List.of());

    service.executar(30, 1, Integer.MAX_VALUE, 100);

    verify(gateway).listarEmAnalise(
        1,
        214748364700L,
        100);
  }
}