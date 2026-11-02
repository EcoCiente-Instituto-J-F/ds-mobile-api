package br.com.ecociente.mobile.entrypoint.dto.validador;

@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ErroResposta(String detail) {
}
