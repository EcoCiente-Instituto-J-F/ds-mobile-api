package br.com.ecociente.mobile.core.gateway.calendario;

import java.util.Optional;

import br.com.ecociente.mobile.core.domain.calendario.Coleta;
import br.com.ecociente.mobile.core.domain.calendario.NovaColeta;
import br.com.ecociente.mobile.core.domain.calendario.StatusType;

public interface ColetaGateway {

  boolean usuarioAtivo(Integer usuarioId);

  boolean condominioAtivo(Integer condominioId);

  boolean cooperativaAtiva(Integer cooperativaId);

  boolean sindicoResponsavel(
      Integer usuarioId,
      Integer condominioId);

  boolean responsavelCooperativa(
      Integer usuarioId,
      Integer cooperativaId);

  Coleta criar(
      Integer usuarioId,
      String origem,
      NovaColeta entrada);

  Optional<Coleta> buscarComBloqueio(Integer coletaId);

  Coleta atualizarStatus(
      Integer coletaId,
      StatusType status,
      Integer respondidoPorUsuarioId);
}