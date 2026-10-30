package br.com.ecociente.mobile.entrypoint.dto.cadastro.response;

import lombok.Builder;

@Builder
public record MoradorCadastroResponseDto(
  Integer id,
  String tipo,
  String nomeCompleto,
  String email,
  String cpf,
  String dataNascimento,
  CondominioDto condominio,
  String registroEm
) {
  @Builder
  public record CondominioDto(
    String codigo,
    String nome,
    EnderecoDto enderecoDto
  ){
    @Builder 
    public record EnderecoDto(
      String cep,
      String estado,
      String cidade,
      String logradouro,
      String numero,
      String complemento
) {
  
}
  }
}
