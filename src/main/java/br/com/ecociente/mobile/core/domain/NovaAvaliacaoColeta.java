package br.com.ecociente.mobile.core.domain;

import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException;
import br.com.ecociente.mobile.core.exception.AvaliacaoColetaException.Tipo;

/**
 * Pedido de avaliação já validado: só existe se o usuário, o agendamento, a nota e o
 * comentário estiverem corretos. O comentário em branco vira nulo.
 */
public record NovaAvaliacaoColeta(
    Integer usuarioId,
    Integer agendamentoId,
    int nota,
    String comentario
) {

  public static final int NOTA_MINIMA = 1;
  public static final int NOTA_MAXIMA = 5;
  public static final int COMENTARIO_MAXIMO = 500;

  public static NovaAvaliacaoColeta de(
      Integer usuarioId,
      Integer agendamentoId,
      Integer nota,
      String comentario) {
    if (usuarioId == null || usuarioId <= 0) {
      throw erro(
          Tipo.NAO_PERMITIDO,
          "USUARIO_INVALIDO",
          "Usuário inválido");
    }

    if (agendamentoId == null || agendamentoId <= 0) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "AGENDAMENTO_ID_INVALIDO",
          "Informe um agendamento válido");
    }

    if (nota == null || nota < NOTA_MINIMA || nota > NOTA_MAXIMA) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "NOTA_INVALIDA",
          "A nota deve ser de 1 a 5");
    }

    String observacao = comentario == null || comentario.isBlank()
        ? null
        : comentario.trim();

    if (observacao != null && observacao.length() > COMENTARIO_MAXIMO) {
      throw erro(
          Tipo.DADOS_INVALIDOS,
          "COMENTARIO_INVALIDO",
          "O comentário deve ter até 500 caracteres");
    }

    return new NovaAvaliacaoColeta(usuarioId, agendamentoId, nota, observacao);
  }

  private static AvaliacaoColetaException erro(
      Tipo tipo,
      String codigo,
      String mensagem) {
    return new AvaliacaoColetaException(
        tipo,
        codigo,
        mensagem);
  }
}
