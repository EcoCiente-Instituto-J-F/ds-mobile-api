package br.com.ecociente.mobile.entrypoint.dto.validador.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Voto de um morador: "aprovar" ou "denunciar" (denúncia exige motivo_denuncia_id). */
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public record VotoRequest(
    @io.swagger.v3.oas.annotations.media.Schema(description = "ID do usuário. Nas rotas do validador, deve corresponder ao usuário autenticado.", example = "20")
    @NotNull @Min(1) Integer usuarioId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Tipo do voto: aprovar ou denunciar.", example = "aprovar")
    @NotNull @Pattern(regexp = "aprovar|denunciar") String tipo,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID do motivo da denúncia. Necessário quando o voto for denunciar.", example = "1")
    @Min(1) Integer motivoDenunciaId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Comentário opcional. Respeite o limite definido neste campo.", example = "Coleta realizada conforme combinado.")
    @Size(max = 255) String comentario
) {
}
