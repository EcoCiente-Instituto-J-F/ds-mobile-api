package br.com.ecociente.mobile.entrypoint.dto.cadastro.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AtualizarCondominioRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Nome a atualizar. Quando omitido, mantém o valor atual.", example = "Nome atualizado")
    @Size(min = 1, max = 255)
    @Pattern(
        regexp = "(?s).*\\S.*",
        message = "Nome não pode estar em branco"
    )
    String nome,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID de um tipo de condomínio existente.", example = "1")
    @Positive
    Integer tipoCondominioId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "URL HTTP ou HTTPS da foto. Em atualização, string vazia limpa a foto quando permitido.", example = "https://storage.example.com/foto.jpg")
    @Size(max = 500)
    @Pattern(
        regexp = "^$|(?i:https?)://\\S+",
        message = "Informe uma URL HTTP ou HTTPS"
    )
    String urlFoto
) {

    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejeitarCampoDesconhecido(
        String campo,
        Object valor
    ) {
        throw new IllegalArgumentException(
            "Campo não permitido: " + campo
        );
    }
}