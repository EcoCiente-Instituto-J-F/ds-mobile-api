package br.com.ecociente.mobile.core.usecase.cadastro.output;

import br.com.ecociente.mobile.core.domain.cadastro.Cooperativa;
import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.Usuario;

public record CadastroCooperativaOutput(
  Usuario usuario,
  Cooperativa cooperativa,
  Endereco endereco
) {
  
}
