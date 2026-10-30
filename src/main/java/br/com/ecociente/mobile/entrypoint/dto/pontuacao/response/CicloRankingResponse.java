package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

import java.time.OffsetDateTime;

public record CicloRankingResponse(
    String id,
    OffsetDateTime inicio,
    OffsetDateTime fim
) {
}