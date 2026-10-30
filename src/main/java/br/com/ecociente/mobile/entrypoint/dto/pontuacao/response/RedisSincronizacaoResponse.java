package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

public record RedisSincronizacaoResponse(
    boolean sincronizado,
    String status
) {
}