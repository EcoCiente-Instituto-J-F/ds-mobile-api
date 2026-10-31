package br.com.ecociente.mobile.dataprovider.gateway.cadastro;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.exception.cadastro.CepException;
import br.com.ecociente.mobile.core.gateway.cadastro.CepGateway;
import br.com.ecociente.mobile.dataprovider.dto.cadastro.ViaCepResponse;

@Component
public class CepGatewayImpl implements CepGateway {

  private final RestTemplate restTemplate;

  public CepGatewayImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public Endereco buscarEnderecoPorCep(String cep) {
    String cepLimpo = cep.replaceAll("\\D", "");
    String url = "https://viacep.com.br/ws/" + cepLimpo + "/json/";

    ViaCepResponse resposta;

    try {
      resposta = restTemplate.getForObject(
          url,
          ViaCepResponse.class);
    } catch (RestClientException exception) {
      throw new CepException(
          "CEP_EXCEPTION_UNAVAILABLE",
          "Consulta de CEP indisponível");
    }

    if (resposta == null) {
      throw new CepException(
          "CEP_EXCEPTION_UNAVAILABLE",
          "Consulta de CEP indisponível");
    }

    return Endereco.builder()
        .idEndereco(null)
        .cep(resposta.cep())
        .estado(resposta.estado())
        .cidade(resposta.cidade())
        .logradouro(resposta.logradouro())
        .numero(null)
        .complemento(null)
        .build();
  }
}