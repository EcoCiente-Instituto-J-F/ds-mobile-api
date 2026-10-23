package br.com.ecociente.mobile.entrypoint.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliarColetaRequestDto(
    @NotNull(message = "A nota é obrigatória")
    @Min(value = 1, message = "A nota deve ser de 1 a 5")
    @Max(value = 5, message = "A nota deve ser de 1 a 5")
    Integer nota,

    @Size(max = 500, message = "O comentário deve ter até 500 caracteres")
    String comentario) {
}
