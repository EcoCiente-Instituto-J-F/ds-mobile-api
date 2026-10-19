package br.com.ecociente.mobile.entrypoint.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AvaliacoesCooperativaResponseDto(
    Integer cooperativaId,
    BigDecimal media,
    long total,
    int pagina,
    int tamanho,
    long totalPaginas,
    List<AvaliacaoResponseDto> avaliacoes) {

  public record AvaliacaoResponseDto(
      Integer id,
      Integer visitaId,
      Integer agendamentoId,
      Integer condominioId,
      String nomeCondominio,
      int nota,
      String comentario,
      OffsetDateTime avaliadoEm) {
  }
}