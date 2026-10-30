package br.com.ecociente.mobile.core.gateway.pontuacao;

import java.time.OffsetDateTime;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.pontuacao.CategoriaResiduo;

public interface CategoriaResiduoGateway {

  Optional<CategoriaResiduo> buscarPorId(Integer categoriaId);

  Integer calcularPontosDisponiveis(
      Integer usuarioId,
      Integer categoriaId,
      OffsetDateTime dataReferencia,
      String timezone);
}