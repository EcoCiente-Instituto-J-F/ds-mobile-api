package br.com.ecociente.mobile.core.usecase.cadastro.input;

import java.time.LocalDate;

import br.com.ecociente.mobile.core.usecase.cadastro.output.CadastroMoradorOutput;

public interface CadastrarMoradorUseCase {
  CadastroMoradorOutput executar(
    String nomeCompleto, 
    String email, 
    String senha, 
    LocalDate dataNascimento, 
    String cpf, 
    String codigoCondominio);
}
