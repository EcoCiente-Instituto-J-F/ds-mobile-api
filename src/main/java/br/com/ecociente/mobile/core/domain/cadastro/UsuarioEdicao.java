package br.com.ecociente.mobile.core.domain.cadastro;

import java.time.LocalDate;

public record UsuarioEdicao(
    Integer id,
    String tipo,
    Integer enderecoId,
    LocalDate dataNascimento
) {
}