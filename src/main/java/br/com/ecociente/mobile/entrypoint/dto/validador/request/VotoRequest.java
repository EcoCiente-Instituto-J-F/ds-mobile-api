package br.com.ecociente.mobile.entrypoint.dto.validador.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Voto de um morador: "aprovar" ou "denunciar" (denúncia exige motivo_denuncia_id). */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VotoRequest(
    @NotNull @Min(1) Integer usuarioId,
    @NotNull @Pattern(regexp = "aprovar|denunciar") String tipo,
    @Min(1) Integer motivoDenunciaId,
    @Size(max = 255) String comentario) {
}
