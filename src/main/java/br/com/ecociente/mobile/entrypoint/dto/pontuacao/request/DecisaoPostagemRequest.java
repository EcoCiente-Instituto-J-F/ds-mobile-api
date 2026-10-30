package br.com.ecociente.mobile.entrypoint.dto.pontuacao.request;

import jakarta.validation.constraints.NotNull;

public record DecisaoPostagemRequest(
    @NotNull(message = "A decisão é obrigatória") Boolean aprovar) {
}
