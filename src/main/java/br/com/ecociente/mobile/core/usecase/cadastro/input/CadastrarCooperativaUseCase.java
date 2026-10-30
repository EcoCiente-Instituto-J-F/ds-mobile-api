package br.com.ecociente.mobile.core.usecase.cadastro.input;


import br.com.ecociente.mobile.core.usecase.cadastro.output.CadastroCooperativaOutput;

public interface CadastrarCooperativaUseCase {
  CadastroCooperativaOutput executar(
    String nomeResponsavel, 
    String email, 
    String senha, 
    String nomeCooperativa,
    String cnpj,
    String emailCooperativa,
    String telefone,
    String cep, 
    String numero, 
    String complemento);
}
