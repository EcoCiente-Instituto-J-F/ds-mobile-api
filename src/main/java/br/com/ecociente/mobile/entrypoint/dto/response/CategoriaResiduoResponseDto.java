package br.com.ecociente.mobile.entrypoint.dto.response;

public record CategoriaResiduoResponseDto(
    Integer id,
    String nome,
    String descricao,
    int pontosBase) {
}
