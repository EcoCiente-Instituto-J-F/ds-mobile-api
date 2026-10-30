package br.com.ecociente.mobile.core.gateway.pontuacao;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.pontuacao.Postagem;

public interface PostagemGateway {

  Optional<Integer> buscarUsuarioId(Integer postagemId);

  boolean bloquearUsuario(Integer usuarioId);

  Optional<Postagem> buscarComBloqueio(Integer postagemId);

  void marcarReconciliacaoConcluida(Integer postagemId);

  Optional<Integer> buscarCondominioId(Integer postagemId);
}