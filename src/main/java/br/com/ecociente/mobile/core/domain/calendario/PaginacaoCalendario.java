package br.com.ecociente.mobile.core.domain.calendario;

import java.util.List;
import java.util.Objects;

public record PaginacaoCalendario(
        int pagina,
        int tamanho,
        boolean paginada,
        List<OrdenacaoCalendario> ordenacoes) {

    public PaginacaoCalendario {
        if (pagina < 0) {
            throw new IllegalArgumentException(
                    "A página não pode ser negativa");
        }

        if (tamanho < 1) {
            throw new IllegalArgumentException(
                    "O tamanho da página deve ser positivo");
        }

        ordenacoes = List.copyOf(
                Objects.requireNonNull(ordenacoes));
    }

    public long deslocamento() {
        return paginada ? (long) pagina * tamanho : 0L;
    }
}