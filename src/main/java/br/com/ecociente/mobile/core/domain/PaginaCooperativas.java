package br.com.ecociente.mobile.core.domain;

import java.util.List;

public record PaginaCooperativas(
    long total,
    int pagina,
    int tamanho,
    long totalPaginas,
    List<Cooperativa> cooperativas) {
}
