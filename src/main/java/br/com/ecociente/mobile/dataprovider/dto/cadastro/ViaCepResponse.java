package br.com.ecociente.mobile.dataprovider.dto.cadastro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponse(
    String cep,
    @JsonProperty("uf") String estado,
    @JsonProperty("localidade") String cidade,
    String logradouro) {
}