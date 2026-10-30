package br.com.ecociente.mobile.core.usecase.cadastro.input;

import java.time.LocalDate;

import br.com.ecociente.mobile.core.usecase.cadastro.output.CadastroUsuarioComumOutput;

public interface CadastroUsuarioComumUseCase {
  CadastroUsuarioComumOutput executar (
    String nomeCompleto, 
    String email, 
    String senha, 
    LocalDate dataNascimento, 
    String cpf, 
    String cep, 
    String numero, 
    String complemento);
}
