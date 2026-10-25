package br.com.ecociente.mobile.core.domain;

import java.util.List;

public record PaginaCondominiosAtendidos(Integer cooperativaId, long total,
    int pagina, int tamanho, long totalPaginas, List<CondominioAtendido> condominios) {}
