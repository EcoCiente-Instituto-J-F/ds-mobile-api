package br.com.ecociente.mobile.entrypoint.dto.calendario.request;

import br.com.ecociente.mobile.core.domain.calendario.StatusType;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusColetaRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Status do agendamento. A transição depende do estado atual e da parte autenticada.", example = "CONFIRMADO")
    @NotNull(message = "Status é obrigatório")
    StatusType status
) {
}