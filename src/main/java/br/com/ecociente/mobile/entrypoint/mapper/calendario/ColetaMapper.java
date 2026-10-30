package br.com.ecociente.mobile.entrypoint.mapper.calendario;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.calendario.Coleta;
import br.com.ecociente.mobile.core.domain.calendario.NovaColeta;
import br.com.ecociente.mobile.entrypoint.dto.calendario.request.CriarColetaRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.calendario.response.ColetaResponseDto;

@Component
public class ColetaMapper {

  public NovaColeta toDomain(CriarColetaRequestDto request) {
    return new NovaColeta(
        request.condominioId(),
        request.cooperativaId(),
        request.dataInicio(),
        request.dataFim());
  }

  public ColetaResponseDto toResponse(Coleta coleta) {
    return new ColetaResponseDto(
        coleta.id(),
        coleta.condominioId(),
        coleta.cooperativaId(),
        coleta.status(),
        coleta.dataInicio(),
        coleta.dataFim(),
        coleta.possuiRecorrencia(),
        coleta.solicitadoPorUsuarioId(),
        coleta.origemSolicitacao(),
        coleta.respondidoPorUsuarioId(),
        coleta.respondidoEm());
  }
}