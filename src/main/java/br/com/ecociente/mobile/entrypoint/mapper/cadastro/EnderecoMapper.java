package br.com.ecociente.mobile.entrypoint.mapper.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.response.CepResponseDto;

@Component 
public class EnderecoMapper {
  public Endereco toDomain(CepResponseDto cep, String numero, String complemento){
    return new Endereco(
      null,
      cep.cep(),
      cep.estado(),
      cep.cidade(),
      cep.logradouro(),
      numero,
      complemento
    );
  }
}
