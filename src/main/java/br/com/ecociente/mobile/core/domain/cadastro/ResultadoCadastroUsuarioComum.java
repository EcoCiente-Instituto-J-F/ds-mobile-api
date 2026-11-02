package br.com.ecociente.mobile.core.domain.cadastro;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.Usuario;

public record ResultadoCadastroUsuarioComum(
  Usuario usuario, Endereco endereco
) {
  
}
