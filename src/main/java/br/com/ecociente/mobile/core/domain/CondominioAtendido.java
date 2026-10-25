package br.com.ecociente.mobile.core.domain;

import java.util.List;

public record CondominioAtendido(Integer id, String nome, Endereco endereco,
    long totalTorres, List<String> diasColeta) {
  public record Endereco(String cep, String estado, String cidade,
      String logradouro, String numero, String complemento) {}
}
