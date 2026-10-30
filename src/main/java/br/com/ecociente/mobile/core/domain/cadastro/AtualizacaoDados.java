package br.com.ecociente.mobile.core.domain.cadastro;

import java.time.LocalDate;

public record AtualizacaoDados(
    String nome,
    String email,
    LocalDate dataNascimento,
    String urlAvatar,
    String telefone,
    Integer telefoneId
) {
}