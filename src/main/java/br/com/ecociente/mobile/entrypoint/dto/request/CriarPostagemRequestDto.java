package br.com.ecociente.mobile.entrypoint.dto.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CriarPostagemRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "URL HTTP ou HTTPS da foto. Em atualização, string vazia limpa a foto quando permitido.", example = "https://storage.example.com/foto.jpg")
    @NotBlank(message = "A URL da foto é obrigatória")
    @Size(max = 500, message = "A URL da foto deve ter até 500 caracteres")
    String urlFoto,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Hash identificador da foto. Uma foto já utilizada não pode ser cadastrada novamente.", example = "hash-exemplo-foto-001")
    @NotBlank(message = "O hash da foto é obrigatório")
    @Size(max = 128, message = "O hash da foto deve ter até 128 caracteres")
    String hashFoto,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Instante da captura com fuso horário. Não pode estar no futuro.", example = "2026-01-01T10:00:00-03:00")
    @NotNull(message = "A data de captura é obrigatória")
    OffsetDateTime capturadaEm,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID da categoria de resíduo. Consulte o catálogo de categorias.", example = "1")
    @NotNull(message = "A categoria é obrigatória")
    @Positive(message = "Categoria inválida")
    Integer categoriaId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID numérico do condomínio existente no banco.", example = "1")
    @NotNull(message = "O condomínio é obrigatório")
    @Positive(message = "Condomínio inválido")
    Integer condominioId
) {
}
