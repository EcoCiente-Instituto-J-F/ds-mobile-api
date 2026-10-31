package br.com.ecociente.mobile.entrypoint.dto.cadastro.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AtualizarEnderecoRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "CEP com oito dígitos, com ou sem hífen.", example = "01001000")
    @NotBlank(message = "CEP é obrigatório")
    @Pattern(
        regexp = "^\\d{5}-?\\d{3}$",
        message = "CEP inválido"
    )
    String cep,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Número do endereço.", example = "10")
    @NotBlank(message = "Número é obrigatório")
    @Size(max = 20)
    String numero,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Complemento do endereço.", example = "Apartamento 12")
    @Size(max = 100)
    String complemento
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