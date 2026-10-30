package br.com.ecociente.mobile.entrypoint.dto.validador.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Decisão do síndico sobre uma postagem em análise. */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DecisaoRequest(
    @NotNull @Min(1) Integer usuarioId,
    @NotNull Boolean aprovar) {
}
