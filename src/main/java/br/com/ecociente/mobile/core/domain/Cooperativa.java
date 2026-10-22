package br.com.ecociente.mobile.core.domain;

import java.math.BigDecimal;

public record Cooperativa(
    Integer id,
    String nome,
    String email,
    String telefone,
    String cep,
    String logradouro,
    String numero,
    String complemento,
    String cidade,
    String estado,
    BigDecimal mediaAvaliacoes,
    long totalAvaliacoes) {
}
