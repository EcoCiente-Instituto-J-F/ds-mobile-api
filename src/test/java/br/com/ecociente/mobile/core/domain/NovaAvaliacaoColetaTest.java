package br.com.ecociente.mobile.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException.Tipo;

class NovaAvaliacaoColetaTest {

  @Test
  void deveCriarComComentarioSemEspacosSobrando() {
    var avaliacao = NovaAvaliacaoColeta.de(30, 100, 5, "  Pontual  ");

    assertEquals(30, avaliacao.usuarioId());
    assertEquals(100, avaliacao.agendamentoId());
    assertEquals(5, avaliacao.nota());
    assertEquals("Pontual", avaliacao.comentario());
  }

  @ParameterizedTest
  @ValueSource(strings = { "", "   " })
  void comentarioVazioOuEmBrancoDeveVirarNulo(String comentario) {
    assertNull(NovaAvaliacaoColeta.de(30, 100, 4, comentario).comentario());
  }

  @Test
  void comentarioAusenteDeveFicarNulo() {
    assertNull(NovaAvaliacaoColeta.de(30, 100, 4, null).comentario());
  }

  @Test
  void deveAceitarComentarioNoLimite() {
    assertDoesNotThrow(
        () -> NovaAvaliacaoColeta.de(30, 100, 5, "a".repeat(500)));
  }

  @ParameterizedTest
  @ValueSource(ints = { 1, 2, 3, 4, 5 })
  void deveAceitarNotaDeUmACinco(int nota) {
    assertEquals(nota, NovaAvaliacaoColeta.de(30, 100, nota, null).nota());
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -1, 6, 100 })
  void deveRejeitarNotaForaDoIntervalo(int nota) {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> NovaAvaliacaoColeta.de(30, 100, nota, null));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("NOTA_INVALIDA", erro.getCodigo());
  }

  @Test
  void deveRejeitarNotaAusente() {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> NovaAvaliacaoColeta.de(30, 100, null, null));

    assertEquals("NOTA_INVALIDA", erro.getCodigo());
  }

  @Test
  void deveRejeitarComentarioMaiorQueOLimite() {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> NovaAvaliacaoColeta.de(30, 100, 5, "a".repeat(501)));

    assertEquals("COMENTARIO_INVALIDO", erro.getCodigo());
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -5 })
  void deveRejeitarUsuarioInvalido(int usuario) {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> NovaAvaliacaoColeta.de(usuario, 100, 5, null));

    assertEquals(Tipo.NAO_PERMITIDO, erro.getTipo());
    assertEquals("USUARIO_INVALIDO", erro.getCodigo());
  }

  @ParameterizedTest
  @ValueSource(ints = { 0, -1 })
  void deveRejeitarAgendamentoInvalido(int agendamento) {
    var erro = assertThrows(
        AvaliacaoColetaException.class,
        () -> NovaAvaliacaoColeta.de(30, agendamento, 5, null));

    assertEquals(Tipo.DADOS_INVALIDOS, erro.getTipo());
    assertEquals("AGENDAMENTO_ID_INVALIDO", erro.getCodigo());
  }
}
