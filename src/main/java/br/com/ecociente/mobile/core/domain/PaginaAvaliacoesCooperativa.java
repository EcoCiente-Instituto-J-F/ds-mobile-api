package br.com.ecociente.mobile.core.domain;

import java.math.BigDecimal;
import java.util.List;

public record PaginaAvaliacoesCooperativa(
    Integer cooperativaId,
    BigDecimal media,
    long total,
    int pagina,
    int tamanho,
    long totalPaginas,
    List<AvaliacaoCooperativa> avaliacoes
) {
}