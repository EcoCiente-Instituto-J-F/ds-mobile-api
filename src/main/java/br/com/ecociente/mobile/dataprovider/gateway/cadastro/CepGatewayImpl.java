package br.com.ecociente.mobile.dataprovider.gateway.cadastro;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.exception.cadastro.CepException;
import br.com.ecociente.mobile.core.gateway.cadastro.CepGateway;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.response.CepResponseDto;

@Component 
public class CepGatewayImpl implements CepGateway {
  private final RestTemplate restTemplate;

  public CepGatewayImpl(RestTemplate restTemplate){
    this.restTemplate = restTemplate;
  }

  @Override
  public Endereco buscarEnderecoPorCep(String cep) {
    String cepLimpo = cep.replaceAll("\\D","");
    String url = "https://viacep.com.br/ws/" + cepLimpo + "/json/";

    CepResponseDto responseDto;
    try{
      responseDto = restTemplate.getForObject(url, CepResponseDto.class);
    }catch(Exception e){
      throw new CepException("CEP_EXCEPTION_UNAVAILABLE","Consulta de CEP indisponível");
    }
    if (responseDto == null) {
      throw new CepException("CEP_EXCEPTION_UNAVAILABLE","Consulta de CEP indisponível");      
    }

    return Endereco.builder()
      .idEndereco(null)
      .cep(responseDto.cep())
      .estado(responseDto.estado())
      .cidade(responseDto.cidade())
      .logradouro(responseDto.logradouro())
      .numero(null)
      .complemento(null)
      .build();
  }

  

  
}
