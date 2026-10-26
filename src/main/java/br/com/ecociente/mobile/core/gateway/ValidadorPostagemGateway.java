package br.com.ecociente.mobile.core.gateway;

import br.com.ecociente.mobile.core.domain.ResultadoVoto;

public interface ValidadorPostagemGateway {

  ResultadoVoto votar(
      Integer postagemId,
      Integer usuarioId,
      String tipo,
      Integer motivoDenunciaId,
      String comentario);
}
