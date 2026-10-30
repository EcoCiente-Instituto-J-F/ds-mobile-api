package br.com.ecociente.mobile.dataprovider.mapper.autenticacao;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.autenticacao.Usuario;
import br.com.ecociente.mobile.dataprovider.entity.UsuarioEntity;

@Component
public class UsuarioAutenticacaoPersistenceMapper {

  public Usuario toDomain(UsuarioEntity entity) {
    return new Usuario(
        entity.getId(),
        entity.getNomeUsuario(),
        entity.getEmailUsuario(),
        entity.getSenhaHash(),
        entity.getAtivo(),
        entity.getTipoUsuario().getNomeTipo());
  }
}