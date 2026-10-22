package br.com.ecociente.mobile.core.domain;

import java.util.List;

public record PaginaPostagens(
    long total,
    int pagina,
    int tamanho,
    long totalPaginas,
    List<Postagem> postagens) {
}
