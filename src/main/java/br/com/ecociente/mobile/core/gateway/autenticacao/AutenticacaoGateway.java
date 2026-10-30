package br.com.ecociente.mobile.core.gateway.autenticacao;

import br.com.ecociente.mobile.core.domain.autenticacao.Autenticacao;

public interface AutenticacaoGateway {
  Autenticacao salvar (Autenticacao autenticacao);
}
