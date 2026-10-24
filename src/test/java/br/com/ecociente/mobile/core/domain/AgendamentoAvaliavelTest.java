package br.com.ecociente.mobile.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AgendamentoAvaliavelTest {

  @Test
  void deveReconhecerOSindicoDoCondominio() {
    assertTrue(new AgendamentoAvaliavel(1, 30, true).eDoSindico(30));
  }

  @Test
  void naoDeveReconhecerOutroUsuario() {
    assertFalse(new AgendamentoAvaliavel(1, 30, true).eDoSindico(31));
  }

  @Test
  void condominioSemSindicoNaoTemQuemAvalie() {
    assertFalse(new AgendamentoAvaliavel(1, null, true).eDoSindico(30));
    assertFalse(new AgendamentoAvaliavel(1, null, true).eDoSindico(null));
  }
}
