package br.com.ecociente.mobile.entrypoint.dto.calendario.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CriarColetaRequestDto(

    @NotNull(message = "Condomínio é obrigatório")
    @Positive(message = "Condomínio deve ser positivo")
    Integer condominioId,

    @NotNull(message = "Cooperativa é obrigatória")
    @Positive(message = "Cooperativa deve ser positiva")
    Integer cooperativaId,

    @NotNull(message = "Data de início é obrigatória")
    @Future(message = "Data de início deve estar no futuro")
    OffsetDateTime dataInicio,

    OffsetDateTime dataFim

) {

  @AssertTrue(message = "Data de fim deve ser igual ou posterior ao início")
  public boolean isPeriodoValido() {
    return dataInicio == null
        || dataFim == null
        || !dataFim.isBefore(dataInicio);
  }
}