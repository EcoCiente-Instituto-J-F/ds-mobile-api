package br.com.ecociente.mobile.entrypoint.dto.response;

import java.util.List;

public record CooperativasResponseDto(
        long total,
        int pagina,
        int tamanho,
        long totalPaginas,
        Integer condominioId,
        List<CooperativaResponseDto> cooperativas) {
}
