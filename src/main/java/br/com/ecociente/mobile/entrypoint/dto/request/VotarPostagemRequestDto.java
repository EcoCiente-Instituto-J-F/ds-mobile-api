package br.com.ecociente.mobile.entrypoint.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record VotarPostagemRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Tipo do voto: aprovar ou denunciar.", example = "aprovar")
    @NotBlank(message = "O tipo do voto é obrigatório")
    @Pattern(regexp = "aprovar|denunciar", message = "O tipo do voto deve ser aprovar ou denunciar")
    String tipo,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID do motivo da denúncia. Necessário quando o voto for denunciar.", example = "1")
    @Positive(message = "Motivo de denúncia inválido")
    Integer motivoDenunciaId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Comentário opcional. Respeite o limite definido neste campo.", example = "Coleta realizada conforme combinado.")
    @Size(max = 255, message = "O comentário deve ter até 255 caracteres")
    String comentario
) {
}
