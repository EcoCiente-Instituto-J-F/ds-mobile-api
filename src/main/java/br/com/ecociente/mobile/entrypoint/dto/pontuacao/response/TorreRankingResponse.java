package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

public record TorreRankingResponse(
    Long posicao,
    Integer torreId,
    String nome,
    Long pontos
) {
}