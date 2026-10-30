package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

public record MoradorRankingResponse(
    Long posicao,
    Integer usuarioId,
    String nome,
    Long pontos
) {
}