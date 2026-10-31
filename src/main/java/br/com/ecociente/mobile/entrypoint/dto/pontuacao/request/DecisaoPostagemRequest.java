package br.com.ecociente.mobile.entrypoint.dto.pontuacao.request;

import jakarta.validation.constraints.NotNull;

public record DecisaoPostagemRequest(
    @io.swagger.v3.oas.annotations.media.Schema(description = "true aprova a postagem; false reprova.", example = "true")
    @NotNull(message = "A decisão é obrigatória") Boolean aprovar
) {
}
