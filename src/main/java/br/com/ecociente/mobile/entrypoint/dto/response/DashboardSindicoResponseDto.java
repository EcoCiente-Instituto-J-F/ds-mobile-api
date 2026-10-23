package br.com.ecociente.mobile.entrypoint.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSindicoResponseDto(
    Integer condominioId,
    String nomeCondominio,
    String nomeSindico,
    long totalMoradores,
    MateriaisDto materiais,
    PontosDto pontos,
    QuizzesDto quizzes) {

  public record MateriaisDto(
      long reciclados,
      long emValidacao,
      long aguardandoSindico,
      long reprovadas,
      List<MaterialPorCategoriaDto> porCategoria) {
  }

  public record MaterialPorCategoriaDto(
      Integer categoriaId,
      String categoria,
      String cor,
      long quantidade) {
  }

  public record PontosDto(
      long total,
      long postagens,
      long quizzes,
      long mesAtual) {
  }

  public record QuizzesDto(
      long tentativasConcluidas,
      long tentativasAprovadas,
      long moradoresComQuiz,
      BigDecimal notaMedia,
      BigDecimal taxaAprovacao,
      List<NotaPorQuizDto> porQuiz) {
  }

  public record NotaPorQuizDto(
      Integer quizId,
      String quiz,
      long tentativas,
      long aprovadas,
      BigDecimal notaMedia) {
  }
}
