package br.com.ecociente.mobile.core.domain.cadastro;

import br.com.ecociente.mobile.core.domain.cadastro.Cooperativa;
import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.Usuario;

public record ResultadoCadastroCooperativa(
  Usuario usuario,
  Cooperativa cooperativa,
  Endereco endereco
) {
  
}
