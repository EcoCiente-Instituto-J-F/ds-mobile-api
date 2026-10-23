package br.com.ecociente.mobile.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class DashboardSindicoTest {

  private DashboardSindico dashboardDoSindico(Integer sindico) {
    return new DashboardSindico(
        1, "Condomínio Um", sindico, "Síndico Um", 5,
        new DashboardSindico.Materiais(3, 2, 1, 1, List.of()),
        new DashboardSindico.Pontos(45, 15, 30, 40),
        new DashboardSindico.Quizzes(3, 2, 2, new BigDecimal("73.33"), new BigDecimal("66.67"), List.of()));
  }

  @Test
  void deveReconhecerOSindicoDoCondominio() {
    assertTrue(dashboardDoSindico(30).eDoSindico(30));
  }

  @Test
  void naoDeveReconhecerOutroUsuario() {
    assertFalse(dashboardDoSindico(30).eDoSindico(31));
  }

  @Test
  void condominioSemSindicoNaoTemQuemConsulte() {
    assertFalse(dashboardDoSindico(null).eDoSindico(30));
    assertFalse(dashboardDoSindico(null).eDoSindico(null));
  }
}
