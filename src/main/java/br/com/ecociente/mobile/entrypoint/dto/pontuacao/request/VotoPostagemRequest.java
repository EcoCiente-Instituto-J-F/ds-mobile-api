package br.com.ecociente.mobile.entrypoint.dto.pontuacao.request;

import jakarta.validation.constraints.NotBlank;

public record VotoPostagemRequest(
    @NotBlank String tipoVoto,
    Integer motivoDenunciaId,
    String comentario) {
}