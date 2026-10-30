package br.com.ecociente.mobile.core.gateway.pontuacao;

import java.util.Optional;

public interface ParticipanteRankingGateway {

  Optional<String> buscarNomeMorador(Integer usuarioId);

  Optional<String> buscarNomeTorre(
      Integer torreId,
      Integer condominioId);
}