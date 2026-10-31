package br.com.ecociente.mobile.entrypoint.dto.pontuacao.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para reconstruir os rankings a partir do PostgreSQL")
public record ReconstruirRankingRequest(
    @io.swagger.v3.oas.annotations.media.Schema(description = "Escopo dos rankings a reconstruir.", example = "TODOS")
    @NotNull(message = "O tipo de ranking é obrigatório")
    
    EscopoReconstrucaoType tipo,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID numérico do condomínio existente no banco.", example = "1")
    @NotNull(message = "O condomínio é obrigatório")
    @Positive(message = "O condomínio deve possuir ID positivo")
    
    Integer condominioId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Ciclo a reconstruir. Atualmente aceita somente ATUAL.", example = "ATUAL", allowableValues = {"ATUAL"})
    @NotBlank(message = "O ciclo é obrigatório")
    @Size(
        max = 40,
        message = "O ciclo deve possuir no máximo 40 caracteres"
    )
    @Pattern(
        regexp = "(?i)ATUAL",
        message = "Somente o ciclo ATUAL pode ser reconstruído"
    )
    
    String ciclo
) {
}