package br.com.ecociente.mobile.entrypoint.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FinalizarTentativaQuizRequestDto(

    @NotEmpty(message = "Informe as respostas")
    List<@NotNull @Valid RespostaRequestDto> respostas

) {

    public record RespostaRequestDto(

        @NotNull(message = "Pergunta é obrigatória")
        @Positive(message = "Pergunta deve ser positiva")
        Integer perguntaId,

        @NotNull(message = "Alternativa é obrigatória")
        @Positive(message = "Alternativa deve ser positiva")
        Integer alternativaId

    ) {
    }
}