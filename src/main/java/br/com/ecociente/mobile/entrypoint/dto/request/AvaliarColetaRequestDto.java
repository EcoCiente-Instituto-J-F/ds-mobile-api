package br.com.ecociente.mobile.entrypoint.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliarColetaRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Nota inteira da avaliação, de 1 a 5.", example = "5")
    @NotNull(message = "A nota é obrigatória")
    @Min(value = 1, message = "A nota deve ser de 1 a 5")
    @Max(value = 5, message = "A nota deve ser de 1 a 5")
    Integer nota,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Comentário opcional. Respeite o limite definido neste campo.", example = "Coleta realizada conforme combinado.")
    @Size(max = 500, message = "O comentário deve ter até 500 caracteres")
    String comentario
) {
}
