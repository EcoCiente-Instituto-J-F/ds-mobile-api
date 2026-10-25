package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException.Tipo;
import br.com.ecociente.mobile.core.gateway.AvaliacaoCooperativaGateway;
import br.com.ecociente.mobile.core.gateway.CondominioAtendidoGateway;

class ConsultarCondominiosAtendidosServiceTest {
  private final AvaliacaoCooperativaGateway cooperativas = mock(AvaliacaoCooperativaGateway.class);
  private final CondominioAtendidoGateway gateway = mock(CondominioAtendidoGateway.class);
  private ConsultarCondominiosAtendidosService service;

  @BeforeEach
  void preparar() { service = new ConsultarCondominiosAtendidosService(cooperativas, gateway); }

  @Test
  void devePreservarTotalAoConsultarPaginaVaziaAlemDoFinal() {
    when(cooperativas.buscarResponsavelAtivo(1)).thenReturn(Optional.of(10));
    when(gateway.contar(1)).thenReturn(3L);
    when(gateway.listar(1, 198L, 2)).thenReturn(List.of());
    var pagina = service.executar(10, 1, 99, 2);
    assertEquals(3L, pagina.total());
    assertEquals(2L, pagina.totalPaginas());
    assertTrue(pagina.condominios().isEmpty());
    verify(gateway).listar(1, 198L, 2);
  }

  @Test
  void deveBloquearOutroResponsavelAntesDeConsultarDados() {
    when(cooperativas.buscarResponsavelAtivo(1)).thenReturn(Optional.of(20));
    var erro = assertThrows(ConsultaCooperativaException.class, () -> service.executar(10, 1, 0, 10));
    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    verifyNoInteractions(gateway);
  }

  @Test
  void deveRetornar404QuandoNaoHaResponsavelAtivo() {
    when(cooperativas.buscarResponsavelAtivo(1)).thenReturn(Optional.empty());
    var erro = assertThrows(ConsultaCooperativaException.class, () -> service.executar(10, 1, 0, 10));
    assertEquals(Tipo.NAO_ENCONTRADO, erro.getTipo());
    verifyNoInteractions(gateway);
  }

  @ParameterizedTest
  @CsvSource({"0,0,10", "1,-1,10", "1,0,0", "1,0,101"})
  void deveRejeitarParametrosInvalidos(int id, int pagina, int tamanho) {
    var erro = assertThrows(ConsultaCooperativaException.class, () -> service.executar(10, id, pagina, tamanho));
    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    verifyNoInteractions(cooperativas, gateway);
  }

  @Test
  void deveRejeitarUsuarioAusente() {
    assertThrows(ConsultaCooperativaException.class, () -> service.executar(null, 1, 0, 10));
    verifyNoInteractions(cooperativas, gateway);
  }

  @Test
  void deveCalcularDeslocamentoSemOverflowDeInteiro() {
    when(cooperativas.buscarResponsavelAtivo(1)).thenReturn(Optional.of(10));
    when(gateway.listar(1, 214748364700L, 100)).thenReturn(List.of());
    service.executar(10, 1, Integer.MAX_VALUE, 100);
    verify(gateway).listar(1, 214748364700L, 100);
  }
}
