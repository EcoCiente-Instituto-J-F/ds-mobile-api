package br.com.ecociente.mobile.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.ecociente.mobile.core.domain.Cooperativa;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException.Tipo;
import br.com.ecociente.mobile.core.gateway.CooperativaDetalheGateway;

class ConsultarCooperativaServiceTest {

  private CooperativaDetalheGateway gateway;
  private ConsultarCooperativaService service;

  @BeforeEach
  void configurar() {
    gateway = mock(CooperativaDetalheGateway.class);
    service = new ConsultarCooperativaService(gateway);
  }

  @Test
  void deveConsultarCooperativaVisivelAoSindico() {
    var cooperativa = cooperativa();

    when(gateway.sindicoTemCondominioAtivo(30))
        .thenReturn(true);

    when(gateway.buscarVisivelAoSindico(30, 1))
        .thenReturn(Optional.of(cooperativa));

    var resultado = service.executar(30, 1);

    assertEquals(cooperativa, resultado);
    assertEquals(new BigDecimal("4.00"), resultado.mediaAvaliacoes());
    assertEquals(3L, resultado.totalAvaliacoes());
  }

  @Test
  void deveBloquearUsuarioSemCondominioAtivo() {
    when(gateway.sindicoTemCondominioAtivo(50))
        .thenReturn(false);

    var erro = assertThrows(
        ConsultaCooperativaException.class,
        () -> service.executar(50, 1));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());

    verify(gateway, never()).buscarVisivelAoSindico(
        anyInt(),
        anyInt());
  }

  @Test
  void deveInformarCooperativaIndisponivel() {
    when(gateway.sindicoTemCondominioAtivo(30))
        .thenReturn(true);

    when(gateway.buscarVisivelAoSindico(30, 999))
        .thenReturn(Optional.empty());

    var erro = assertThrows(
        ConsultaCooperativaException.class,
        () -> service.executar(30, 999));

    assertEquals(Tipo.NAO_ENCONTRADO, erro.getTipo());
    assertEquals("COOPERATIVA_NAO_ENCONTRADA", erro.getCodigo());
  }

  @Test
  void deveRejeitarUsuarioInvalido() {
    for (Integer usuario : new Integer[] {null, 0, -1}) {
      var erro = assertThrows(
          ConsultaCooperativaException.class,
          () -> service.executar(usuario, 1));

      assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    }

    verifyNoInteractions(gateway);
  }

  @Test
  void deveRejeitarIdDaCooperativaInvalido() {
    for (Integer id : new Integer[] {null, 0, -1}) {
      var erro = assertThrows(
          ConsultaCooperativaException.class,
          () -> service.executar(30, id));

      assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    }

    verifyNoInteractions(gateway);
  }

  private Cooperativa cooperativa() {
    return new Cooperativa(
        1,
        "Cooperativa Um",
        "contato@teste.com",
        "11999990000",
        "01001000",
        "Rua da Cooperativa",
        "100",
        null,
        "São Paulo",
        "SP",
        new BigDecimal("4.00"),
        3L);
  }
}