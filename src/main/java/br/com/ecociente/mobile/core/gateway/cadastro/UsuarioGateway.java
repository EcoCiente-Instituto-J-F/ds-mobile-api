package br.com.ecociente.mobile.core.gateway.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Usuario;

@Component 
public interface UsuarioGateway {
  Usuario salvar(Usuario usuario);
  boolean existePorEmail(String email);
  boolean existePorCpf(String cpf);
}
