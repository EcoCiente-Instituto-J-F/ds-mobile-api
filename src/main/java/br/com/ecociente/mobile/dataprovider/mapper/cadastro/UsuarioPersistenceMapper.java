package br.com.ecociente.mobile.dataprovider.mapper.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Usuario;
import br.com.ecociente.mobile.dataprovider.entity.UsuarioEntity;

@Component 
public class UsuarioPersistenceMapper {

  public UsuarioEntity toEntity(Usuario usuario){
    return UsuarioEntity.builder()
    .id(usuario.getIdUsuario())
    .nomeUsuario(usuario.getNomeUsuario())
    .emailUsuario(usuario.getEmaiUsuario())
    .senhaHash(usuario.getSenhaHash())
    .dataNascimento(usuario.getDataNascimento())
    .cpf(usuario.getCpf())
    .urlAvatar(usuario.getUrlAvatar())
    .ativo(usuario.getAtivo())
    .registroEm(usuario.getRegistroEm())
    .tipoUsuarioId(usuario.getTipoUsuarioId())
    .enderecoId(usuario.getEnderecoId())
    .build();
  }

  public Usuario toDomain(UsuarioEntity userEntity){
    return Usuario.builder()
    .idUsuario(userEntity.getId())
    .nomeUsuario(userEntity.getNomeUsuario())
    .emaiUsuario(userEntity.getEmailUsuario())
    .senhaHash(userEntity.getSenhaHash())
    .dataNascimento(userEntity.getDataNascimento())
    .cpf(userEntity.getCpf())
    .urlAvatar(userEntity.getUrlAvatar())
    .ativo(userEntity.getAtivo())
    .registroEm(userEntity.getRegistroEm())
    .tipoUsuarioId(userEntity.getTipoUsuarioId())
    .enderecoId(userEntity.getEnderecoId())
    .build();
  }
  
}
