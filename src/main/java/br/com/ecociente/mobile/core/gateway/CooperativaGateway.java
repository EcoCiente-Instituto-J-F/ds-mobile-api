package br.com.ecociente.mobile.core.gateway;

import java.util.List;

import br.com.ecociente.mobile.core.domain.Cooperativa;

public interface CooperativaGateway {

  /** O usuário é síndico de pelo menos um condomínio ativo. */
  boolean sindicoTemCondominioAtivo(Integer usuarioId);

  /** Cooperativas na mesma cidade e estado de algum condomínio ativo do síndico. */
  long contarProximas(Integer usuarioId);

  List<Cooperativa> listarProximas(
      Integer usuarioId,
      long deslocamento,
      int limite);
}
