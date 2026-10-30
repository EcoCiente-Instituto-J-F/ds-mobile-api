package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

import java.util.List;

import br.com.ecociente.mobile.core.domain.pontuacao.TipoRankingType;

public record RankingResponse<T>(
    TipoRankingType tipo,
    Integer condominioId,
    CicloRankingResponse ciclo,
    List<T> ranking
) {
}