package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.AvaliacaoColeta;
import br.com.ecociente.mobile.entrypoint.dto.response.AvaliacaoColetaResponseDto;

@Component
public class AvaliacaoColetaMapper {

  public AvaliacaoColetaResponseDto toResponse(
      AvaliacaoColeta avaliacao) {
    return new AvaliacaoColetaResponseDto(
        avaliacao.id(),
        avaliacao.agendamentoId(),
        avaliacao.visitaId(),
        avaliacao.nota(),
        avaliacao.comentario(),
        avaliacao.avaliadoEm());
  }
}
