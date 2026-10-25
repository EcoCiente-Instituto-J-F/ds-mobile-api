package br.com.ecociente.mobile.core.domain;

import java.util.List;

public record PaginaChat<T>(long total, int pagina, int tamanho, long totalPaginas, List<T> itens) {}
