package br.com.ecociente.mobile.core.gateway.pontuacao;

public interface VinculoCondominialGateway {

  boolean possuiVinculoAtivo(
      Integer usuarioId,
      Integer condominioId);

  boolean torrePertenceCondominio(
      Integer torreId,
      Integer condominioId);

  boolean ehSindico(
      Integer usuarioId,
      Integer condominioId);

  boolean condominioExiste(Integer condominioId);
}
