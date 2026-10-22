package br.com.ecociente.mobile.entrypoint.dto.response;

import java.util.List;

public record PostagensResponseDto(
    long total,
    int pagina,
    int tamanho,
    long totalPaginas,
    List<PostagemResponseDto> postagens) {
}
