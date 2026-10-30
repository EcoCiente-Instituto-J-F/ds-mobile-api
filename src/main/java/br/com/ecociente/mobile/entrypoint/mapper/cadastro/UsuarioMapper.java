package br.com.ecociente.mobile.entrypoint.mapper.cadastro;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.Usuario;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.request.ComumCadastroRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.request.CooperativaCadastroResquestDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.request.MoradorCadastroRequestDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.response.ComumCadastroResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.response.CooperativaCadastroResponseDto;
import br.com.ecociente.mobile.entrypoint.dto.cadastro.response.MoradorCadastroResponseDto;

@Component 
public class UsuarioMapper {

  public Usuario toDomain(ComumCadastroRequestDto requestDto, String senhaHash){
    return new Usuario(
      null,
      requestDto.nomeUsuario(),
      requestDto.email().trim().toLowerCase(),
      senhaHash,
      LocalDate.parse(requestDto.dataNascimento(), DateTimeFormatter.ISO_LOCAL_DATE),
      requestDto.cpf(),
      null,
      true,
      null,
      null,
      null
    );
  }

  public Usuario toDomain(MoradorCadastroRequestDto moradorCadastroRequestDto, String senhaHash, Integer tipoUsuario){
    return new Usuario(
      null,
      moradorCadastroRequestDto.nomeUsuario(),
      moradorCadastroRequestDto.email().trim().toLowerCase(),
      senhaHash,
      LocalDate.parse(moradorCadastroRequestDto.dataNascimento(),DateTimeFormatter.ISO_LOCAL_DATE),
      moradorCadastroRequestDto.cpf(),
      null,
      true,
      null,
      tipoUsuario,
      null
    );
  }

  public Usuario toDomain(CooperativaCadastroResquestDto cooperativaCadastroResquestDto, String senhaHash){
    return new Usuario(
      null,
      cooperativaCadastroResquestDto.nomeResponsavel(),
      cooperativaCadastroResquestDto.email().trim().toLowerCase(),
      senhaHash,
      null,
      null,
      null,
      true,
      null,
      null,
      null
    );
  }

  public ComumCadastroResponseDto toComumCadastroResponse(Usuario usuario, Endereco endereco){
    return ComumCadastroResponseDto.builder()
      .id(usuario.getIdUsuario())
      .tipo("comum")
      .nomeCompleto(usuario.getNomeUsuario())
      .email(usuario.getEmaiUsuario())
      .cpf(usuario.getCpf())
      .dataNascimento(usuario.getDataNascimento().toString())
      .endereco(ComumCadastroResponseDto.EnderecoDto.builder()
        .cep(endereco.getCep())
        .estado(endereco.getEstado())
        .cidade(endereco.getCidade())
        .logradouro(endereco.getLogradouro())
        .numero(endereco.getNumero())
        .complemento(endereco.getComplemento())
        .build())
      .registroEm(usuario.getRegistroEm().toString())
      .build();   
  }

  public MoradorCadastroResponseDto toMoradorCadastroResponse(Usuario usuario, String codigoCondominio, String nomeCondominio, Endereco enderecoCondominio){
    return  MoradorCadastroResponseDto.builder()
    .id(usuario.getIdUsuario())
    .tipo("morador")
    .nomeCompleto(usuario.getNomeUsuario())
    .email(usuario.getEmaiUsuario())
    .cpf(usuario.getCpf())
    .dataNascimento(usuario.getDataNascimento().toString())
    .condominio(MoradorCadastroResponseDto.CondominioDto.builder()
      .codigo(codigoCondominio)
      .nome(nomeCondominio)
      .enderecoDto(MoradorCadastroResponseDto.CondominioDto.EnderecoDto.builder()
        .cep(enderecoCondominio.getCep())
        .estado(enderecoCondominio.getEstado())
        .logradouro(enderecoCondominio.getLogradouro())
        .numero(enderecoCondominio.getNumero())
        .complemento(enderecoCondominio.getComplemento())
        .build())
        .build())
    .registroEm(usuario.getRegistroEm().toString())
    .build();    
  }

  public CooperativaCadastroResponseDto toCooperativaCadastroResponse(Usuario usuario, String nomeCooperativa, String cnpj, String emailCooperativa, String telefone, Endereco endereco){
    return CooperativaCadastroResponseDto.builder()
      .id(usuario.getIdUsuario())
      .tipo("cooperativa")
      .nomeResponsavel(usuario.getNomeUsuario())
      .email(usuario.getEmaiUsuario())
      .cooperativa(CooperativaCadastroResponseDto.CooperativaResponseDTO.builder()
        .nome(nomeCooperativa)
        .cnpj(cnpj)
        .email(emailCooperativa)
        .telefone(telefone)
        .endreco(CooperativaCadastroResponseDto.CooperativaResponseDTO.EnderecoDto.builder()
          .cep(endereco.getCep())
          .estado(endereco.getEstado())
          .cidade(endereco.getCidade())
          .logradouro(endereco.getLogradouro())
          .numero(endereco.getNumero())
          .complemento(endereco.getComplemento())
          .build())
        .build())
      .registroEm(usuario.getRegistroEm().toString())
      .build();  
  }

  
}
