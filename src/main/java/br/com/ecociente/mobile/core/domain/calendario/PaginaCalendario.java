package br.com.ecociente.mobile.core.domain.calendario;

import java.util.List;
import java.util.Objects;

public record PaginaCalendario<T>(
        List<T> itens,
        long totalItens,
        PaginacaoCalendario paginacao) {

    public PaginaCalendario {
        itens = List.copyOf(Objects.requireNonNull(itens));
        Objects.requireNonNull(paginacao);

        if (totalItens < 0) {
            throw new IllegalArgumentException(
                    "O total de itens não pode ser negativo");
        }
    }
}