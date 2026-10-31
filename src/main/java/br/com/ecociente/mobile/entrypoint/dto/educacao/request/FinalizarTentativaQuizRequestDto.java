package br.com.ecociente.mobile.entrypoint.dto.educacao.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FinalizarTentativaQuizRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Uma resposta por pergunta, cobrindo todas as perguntas da tentativa sem repetições.")
    @NotEmpty(message = "Informe as respostas")
    List<@NotNull @Valid RespostaRequestDto> respostas
) {

    public record RespostaRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "ID de uma pergunta pertencente ao quiz da tentativa.", example = "1")
    @NotNull(message = "Pergunta é obrigatória")
        @Positive(message = "Pergunta deve ser positiva")
        Integer perguntaId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID da alternativa escolhida, pertencente à pergunta informada.", example = "10")
    @NotNull(message = "Alternativa é obrigatória")
        @Positive(message = "Alternativa deve ser positiva")
        Integer alternativaId
) {
    }
}