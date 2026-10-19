package br.com.ecociente.mobile.core.gateway;

import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.AvaliacaoCooperativa;
import br.com.ecociente.mobile.core.domain.ResumoAvaliacoesCooperativa;

public interface AvaliacaoCooperativaGateway {

  Optional<Integer> buscarResponsavelAtivo(
      Integer cooperativaId);

  ResumoAvaliacoesCooperativa consultarResumo(
      Integer cooperativaId);

  List<AvaliacaoCooperativa> listar(
      Integer cooperativaId,
      long deslocamento,
      int limite);
}