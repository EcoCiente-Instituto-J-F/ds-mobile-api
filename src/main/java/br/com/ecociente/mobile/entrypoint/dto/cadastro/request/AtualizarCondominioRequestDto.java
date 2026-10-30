package br.com.ecociente.mobile.entrypoint.dto.cadastro.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AtualizarCondominioRequestDto(

    @Size(min = 1, max = 255)
    @Pattern(
        regexp = "(?s).*\\S.*",
        message = "Nome não pode estar em branco"
    )
    String nome,

    @Positive
    Integer tipoCondominioId,

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