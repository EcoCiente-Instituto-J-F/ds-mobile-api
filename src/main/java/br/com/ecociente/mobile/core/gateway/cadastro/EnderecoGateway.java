package br.com.ecociente.mobile.core.gateway.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;

@Component 
public interface EnderecoGateway {
  Endereco salvar(Endereco endereco);
  Endereco buscarPorId(Integer idEndereco);  
}
