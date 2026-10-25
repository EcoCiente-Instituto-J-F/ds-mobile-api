package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.com.ecociente.mobile.core.domain.Cooperativa;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException.Tipo;
import br.com.ecociente.mobile.core.gateway.CooperativaGateway;

class ConsultarCooperativasProximasServiceTest {

  private CooperativaGateway gateway;
  private ConsultarCooperativasProximasService service;

  @BeforeEach
  void configurar() {
    gateway = mock(CooperativaGateway.class);
    service = new ConsultarCooperativasProximasService(gateway);
  }

  private static Cooperativa cooperativa(Integer id) {
    return new Cooperativa(
        id, "Cooperativa " + id, "c@x.com", "81999990000",
        "01001000", "Rua A", "10", null, "São Paulo", "SP",
        new BigDecimal("4.50"), 2);
  }

  @Test
  void deveListarAsCooperativasDaMesmaCidadeComPaginacao() {
    when(gateway.sindicoTemCondominioAtivo(30)).thenReturn(true);
    when(gateway.contarProximas(30)).thenReturn(25L);
    when(gateway.listarProximas(30, 10L, 10))
        .thenReturn(List.of(cooperativa(1), cooperativa(2)));

    var pagina = service.executar(30, 1, 10);

    assertEquals(25L, pagina.total());
    assertEquals(1, pagina.pagina());
    assertEquals(10, pagina.tamanho());
    assertEquals(3L, pagina.totalPaginas());
    assertEquals(2, pagina.cooperativas().size());
  }

  @Test
  void deveRetornarPaginaVaziaQuandoNaoHaCooperativaNaCidade() {
    when(gateway.sindicoTemCondominioAtivo(30)).thenReturn(true);
    when(gateway.contarProximas(30)).thenReturn(0L);
    when(gateway.listarProximas(30, 0L, 10)).thenReturn(List.of());

    var pagina = service.executar(30, 0, 10);

    assertEquals(0L, pagina.total());
    assertEquals(0L, pagina.totalPaginas());
    assertTrue(pagina.cooperativas().isEmpty());
  }

  @Test
  void naoDeveListarParaQuemNaoEhSindicoDeCondominioAtivo() {
    when(gateway.sindicoTemCondominioAtivo(40)).thenReturn(false);

    var erro = assertThrows(
        ConsultaCooperativaException.class,
        () -> service.executar(40, 0, 10));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    assertEquals("SINDICO_SEM_CONDOMINIO", erro.getCodigo());
    verify(gateway, never()).listarProximas(anyInt(), anyLong(), anyInt());
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
        () -> service.executar(30, pagina, tamanho));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("PAGINACAO_INVALIDA", erro.getCodigo());
    verifyNoInteractions(gateway);
  }

  @Test
  void deveRejeitarUsuarioAusente() {
    var erro = assertThrows(
        ConsultaCooperativaException.class,
        () -> service.executar(null, 0, 10));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    assertEquals("USUARIO_INVALIDO", erro.getCodigo());
    verifyNoInteractions(gateway);
  }
}
