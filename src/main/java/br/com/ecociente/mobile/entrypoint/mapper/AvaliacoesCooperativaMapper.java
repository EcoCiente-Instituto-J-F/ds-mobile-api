package br.com.ecociente.mobile.entrypoint.mapper;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.PaginaAvaliacoesCooperativa;
import br.com.ecociente.mobile.entrypoint.dto.response.AvaliacoesCooperativaResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.response.AvaliacoesCooperativaResponseDto.AvaliacaoResponseDto;

@Component
public class AvaliacoesCooperativaMapper {

  public AvaliacoesCooperativaResponseDto toResponse(
      PaginaAvaliacoesCooperativa pagina) {
    var avaliacoes = pagina.avaliacoes().stream()
        .map(avaliacao -> new AvaliacaoResponseDto(
            avaliacao.id(),
            avaliacao.visitaId(),
            avaliacao.agendamentoId(),
            avaliacao.condominioId(),
            avaliacao.nomeCondominio(),
            avaliacao.nota(),
            avaliacao.comentario(),
            avaliacao.avaliadoEm()))
        .toList();

    return new AvaliacoesCooperativaResponseDto(
        pagina.cooperativaId(),
        pagina.media(),
        pagina.total(),
        pagina.pagina(),
        pagina.tamanho(),
        pagina.totalPaginas(),
        avaliacoes);
  }
}