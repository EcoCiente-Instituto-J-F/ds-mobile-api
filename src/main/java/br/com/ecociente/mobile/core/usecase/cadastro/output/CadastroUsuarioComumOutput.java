package br.com.ecociente.mobile.core.usecase.cadastro.output;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.Usuario;

public record CadastroUsuarioComumOutput(
  Usuario usuario, Endereco endereco
) {
  
}
