package br.com.ecociente.mobile.entrypoint.dto.calendario.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CriarColetaRequestDto(
    @io.swagger.v3.oas.annotations.media.Schema(description = "ID numérico do condomínio existente no banco.", example = "1")
    @NotNull(message = "Condomínio é obrigatório")
    @Positive(message = "Condomínio deve ser positivo")
    Integer condominioId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "ID numérico da cooperativa existente no banco.", example = "1")
    @NotNull(message = "Cooperativa é obrigatória")
    @Positive(message = "Cooperativa deve ser positiva")
    Integer cooperativaId,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Data inicial do período. Na criação de coleta, deve estar no futuro.", example = "2030-01-10T09:00:00-03:00")
    @NotNull(message = "Data de início é obrigatória")
    @Future(message = "Data de início deve estar no futuro")
    OffsetDateTime dataInicio,

    @io.swagger.v3.oas.annotations.media.Schema(description = "Data final do período; não pode ser anterior à data inicial.", example = "2030-01-10T11:00:00-03:00")
    OffsetDateTime dataFim
) {

  @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
  @AssertTrue(message = "Data de fim deve ser igual ou posterior ao início")
  public boolean isPeriodoValido() {
    return dataInicio == null
        || dataFim == null
        || !dataFim.isBefore(dataInicio);
  }
}