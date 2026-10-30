package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

public record PosicaoMoradorResponse(
    Integer usuarioId,
    Integer condominioId,
    Long posicao,
    Long pontos,
    Long totalParticipantes
) {
}