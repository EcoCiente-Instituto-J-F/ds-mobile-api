package br.com.ecociente.mobile.entrypoint.dto.validador;

/** Corpo de erro: {"detail": "..."}, como o serviço antigo, que o app já trata. */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ErroResposta(String detail) {
}
