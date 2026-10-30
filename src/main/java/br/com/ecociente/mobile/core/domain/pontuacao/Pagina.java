package br.com.ecociente.mobile.core.domain.pontuacao;

import java.util.List;

public record Pagina<T>(
    List<T> conteudo,
    int pagina,
    int tamanho,
    long totalElementos,
    int totalPaginas) {
}