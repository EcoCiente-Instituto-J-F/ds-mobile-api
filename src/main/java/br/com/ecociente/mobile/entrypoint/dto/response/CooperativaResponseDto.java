package br.com.ecociente.mobile.entrypoint.dto.response;

import java.math.BigDecimal;

public record CooperativaResponseDto(
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
    String enderecoCompleto,
    BigDecimal mediaAvaliacoes,
    long totalAvaliacoes) {
}
