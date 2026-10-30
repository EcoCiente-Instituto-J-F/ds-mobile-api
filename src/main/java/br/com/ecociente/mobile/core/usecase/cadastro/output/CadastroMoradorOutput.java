package br.com.ecociente.mobile.core.usecase.cadastro.output;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.Usuario;

public record CadastroMoradorOutput(
  Usuario usuario,
  String codigoCondominio,
  String nomeCondominio,
  Endereco enderecoCondominio
) {
  
}
