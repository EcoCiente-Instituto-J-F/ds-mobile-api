package br.com.ecociente.mobile.entrypoint.dto.cadastro.response;

import lombok.Builder;

@Builder 
public record CooperativaCadastroResponseDto(
  Integer id, 
  String tipo,
  String nomeResponsavel,
  String email,
  CooperativaResponseDTO cooperativa,
  String registroEm
) {
  @Builder
  public record CooperativaResponseDTO(
    String nome,
    String cnpj,
    String email,
    String telefone,
    EnderecoDto endreco
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
