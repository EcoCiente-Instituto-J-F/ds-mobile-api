package br.com.ecociente.mobile.core.gateway.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Cooperativa;

@Component 
public interface CooperativaGateway {
  Cooperativa salvar(Cooperativa cooperativa);
  boolean existePorCnpj(String cnpj);
  
}
