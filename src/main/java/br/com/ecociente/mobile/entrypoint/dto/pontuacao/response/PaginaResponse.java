package br.com.ecociente.mobile.entrypoint.dto.pontuacao.response;

import java.util.List;

public record PaginaResponse<T>(
    List<T> conteudo,
    int pagina,
    int tamanho,
    long totalElementos,
    int totalPaginas
) {
}