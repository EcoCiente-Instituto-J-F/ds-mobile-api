package br.com.ecociente.mobile.entrypoint.dto.pontuacao.request;

import jakarta.validation.constraints.NotBlank;

public record VotoPostagemRequest(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Tipo do voto: aprovar ou denunciar.", example = "aprovar")
    @NotBlank String tipoVoto,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID do motivo da denúncia. Necessário quando o voto for denunciar.", example = "1")
    Integer motivoDenunciaId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Comentário opcional. Respeite o limite definido neste campo.", example = "Coleta realizada conforme combinado.")
    String comentario
) {
}