package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

public record PosicaoTorreResponse(
    Integer torreId,
    Integer condominioId,
    Long posicao,
    Long pontos,
    Long totalParticipantes
) {
}