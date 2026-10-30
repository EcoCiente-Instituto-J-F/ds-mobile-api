package br.com.ecociente.mobile.entrypoint.dto.educacao.response;

public record AulaResponseDto(
    Integer id,
    Integer cursoId,
    String titulo,
    String conteudo,
    Integer ordem
) {
}