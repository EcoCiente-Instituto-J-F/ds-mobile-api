package br.com.ecociente.mobile.entrypoint.dto.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CriarPostagemRequestDto(
    @NotBlank(message = "A URL da foto é obrigatória")
    @Size(max = 500, message = "A URL da foto deve ter até 500 caracteres")
    String urlFoto,

    @NotBlank(message = "O hash da foto é obrigatório")
    @Size(max = 128, message = "O hash da foto deve ter até 128 caracteres")
    String hashFoto,

    @NotNull(message = "A data de captura é obrigatória")
    OffsetDateTime capturadaEm,

    @NotNull(message = "A categoria é obrigatória")
    @Positive(message = "Categoria inválida")
    Integer categoriaId,

    @NotNull(message = "O condomínio é obrigatório")
    @Positive(message = "Condomínio inválido")
    Integer condominioId) {
}
