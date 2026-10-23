package br.com.ecociente.mobile.core.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Números do condomínio para o síndico: materiais reciclados, pontos e notas dos quizzes.
 * Vem pronto da view vw_dashboard_sindico, uma linha por condomínio.
 */
public record DashboardSindico(
    Integer condominioId,
    String nomeCondominio,
    Integer sindicoUsuarioId,
    String nomeSindico,
    long totalMoradores,
    Materiais materiais,
    Pontos pontos,
    Quizzes quizzes
) {

  public boolean eDoSindico(Integer usuarioId) {
    return sindicoUsuarioId != null
        && sindicoUsuarioId.equals(usuarioId);
  }

  public record Materiais(
      long reciclados,
      long emValidacao,
      long aguardandoSindico,
      long reprovadas,
      List<MaterialPorCategoria> porCategoria
  ) {
  }

  public record MaterialPorCategoria(
      Integer categoriaId,
      String categoria,
      String cor,
      long quantidade
  ) {
  }

  public record Pontos(
      long total,
      long postagens,
      long quizzes,
      long mesAtual
  ) {
  }

  public record Quizzes(
      long tentativasConcluidas,
      long tentativasAprovadas,
      long moradoresComQuiz,
      BigDecimal notaMedia,
      BigDecimal taxaAprovacao,
      List<NotaPorQuiz> porQuiz
  ) {
  }

  public record NotaPorQuiz(
      Integer quizId,
      String quiz,
      long tentativas,
      long aprovadas,
      BigDecimal notaMedia
  ) {
  }
}
