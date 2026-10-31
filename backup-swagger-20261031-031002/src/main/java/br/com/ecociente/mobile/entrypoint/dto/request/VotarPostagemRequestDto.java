package br.com.ecociente.mobile.entrypoint.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VotarPostagemRequestDto(
    @NotBlank(message = "O tipo do voto é obrigatório")
    @Pattern(regexp = "aprovar|denunciar", message = "O tipo do voto deve ser aprovar ou denunciar")
    String tipo,

    @Positive(message = "Motivo de denúncia inválido")
    Integer motivoDenunciaId,

    @Size(max = 255, message = "O comentário deve ter até 255 caracteres")
    String comentario) {
}
