package br.com.ecociente.mobile.core.gateway;

import java.util.List;
import java.util.Optional;

import br.com.ecociente.mobile.core.domain.Postagem;

public interface PostagemAnaliseGateway {

  Optional<Integer> buscarResponsavelAtivo(
      Integer condominioId);

  long contarEmAnalise(
      Integer condominioId);

  List<Postagem> listarEmAnalise(
      Integer condominioId,
      long deslocamento,
      int limite);
}