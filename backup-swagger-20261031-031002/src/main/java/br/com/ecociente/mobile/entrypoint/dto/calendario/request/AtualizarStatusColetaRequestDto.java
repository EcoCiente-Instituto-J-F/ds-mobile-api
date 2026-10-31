package br.com.ecociente.mobile.entrypoint.dto.calendario.request;

import br.com.ecociente.mobile.core.domain.calendario.StatusType;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusColetaRequestDto(

    @NotNull(message = "Status é obrigatório")
    StatusType status

) {
}