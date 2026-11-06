package br.com.ecociente.mobile.core.domain;

import java.util.List;

public record PaginaCooperativas(
        Integer condominioId,
        long total,
        int pagina,
        int tamanho,
        long totalPaginas,
        List<Cooperativa> cooperativas) {
}
