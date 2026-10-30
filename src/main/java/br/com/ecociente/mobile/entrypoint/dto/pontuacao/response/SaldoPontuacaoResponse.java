package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

public record SaldoPontuacaoResponse(
    Integer usuarioId,
    Long saldo
) {
}