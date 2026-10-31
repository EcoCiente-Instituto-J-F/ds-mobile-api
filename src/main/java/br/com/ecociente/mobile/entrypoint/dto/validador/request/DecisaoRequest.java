package br.com.ecociente.mobile.entrypoint.dto.validador.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Decisão do síndico sobre uma postagem em análise. */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DecisaoRequest(
    @io.swagger.v3.oas.annotations.media.Schema(description = "ID do usuário. Nas rotas do validador, deve corresponder ao usuário autenticado.", example = "20")
    @NotNull @Min(1) Integer usuarioId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "true aprova a postagem; false reprova.", example = "true")
    @NotNull Boolean aprovar
) {
}
