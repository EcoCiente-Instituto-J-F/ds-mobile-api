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
import org.junit.jupiter.params.provider.ValueSource;

import br.com.ecociente.mobile.core.domain.DashboardSindico;
import br.com.ecociente.mobile.core.exception.DashboardSindicoException;
import br.com.ecociente.mobile.core.exception.DashboardSindicoException.Tipo;
import br.com.ecociente.mobile.core.gateway.DashboardSindicoGateway;

class ConsultarDashboardSindicoServiceTest {

  private DashboardSindicoGateway gateway;
  private ConsultarDashboardSindicoService service;

  @BeforeEach
  void configurar() {
    gateway = mock(DashboardSindicoGateway.class);
    service = new ConsultarDashboardSindicoService(gateway);
  }

  private DashboardSindico dashboard(Integer sindico) {
    return new DashboardSindico(
        1, "Condomínio Um", sindico, "Síndico Um", 5,
        new DashboardSindico.Materiais(3, 2, 1, 1, List.of()),
        new DashboardSindico.Pontos(45, 15, 30, 40),
        new DashboardSindico.Quizzes(3, 2, 2, new BigDecimal("73.33"), new BigDecimal("66.67"), List.of()));
  }

  @Test
  void deveDevolverODashboardAoSindicoDoCondominio() {
    when(gateway.buscarPorCondominio(1)).thenReturn(Optional.of(dashboard(30)));

    var resultado = service.executar(30, 1);

    assertEquals(1, resultado.condominioId());
    assertEquals(5, resultado.totalMoradores());
    assertEquals(45, resultado.pontos().total());
  }

  @Test
  void naoDeveDevolverParaOutroUsuario() {
    when(gateway.buscarPorCondominio(1)).thenReturn(Optional.of(dashboard(30)));

    var erro = assertThrows(
        DashboardSindicoException.class,
        () -> service.executar(31, 1));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    assertEquals("DASHBOARD_NAO_PERMITIDO", erro.getCodigo());
  }

  @Test
  void naoDeveDevolverParaCondominioSemSindico() {
    when(gateway.buscarPorCondominio(1)).thenReturn(Optional.of(dashboard(null)));

    var erro = assertThrows(
        DashboardSindicoException.class,
        () -> service.executar(30, 1));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
  }

  @Test
  void deveInformarCondominioNaoEncontrado() {
    when(gateway.buscarPorCondominio(1)).thenReturn(Optional.empty());

    var erro = assertThrows(
        DashboardSindicoException.class,
        () -> service.executar(30, 1));

    assertEquals(Tipo.NAO_ENCONTRADO, erro.getTipo());
    assertEquals("CONDOMINIO_NAO_ENCONTRADO", erro.getCodigo());
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -1 })
  void deveRejeitarCondominioInvalido(int condominio) {
    var erro = assertThrows(
        DashboardSindicoException.class,
        () -> service.executar(30, condominio));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("CONDOMINIO_ID_INVALIDO", erro.getCodigo());

    verifyNoInteractions(gateway);
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -1 })
  void deveRejeitarUsuarioInvalido(int usuario) {
    var erro = assertThrows(
        DashboardSindicoException.class,
        () -> service.executar(usuario, 1));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    assertEquals("USUARIO_INVALIDO", erro.getCodigo());

    verifyNoInteractions(gateway);
  }
}
