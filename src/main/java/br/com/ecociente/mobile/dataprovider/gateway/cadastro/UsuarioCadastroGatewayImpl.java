package br.com.ecociente.mobile.dataprovider.gateway.cadastro;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Usuario;
import br.com.ecociente.mobile.core.gateway.cadastro.UsuarioGateway;
import br.com.ecociente.mobile.dataprovider.entity.UsuarioEntity;
import br.com.ecociente.mobile.dataprovider.mapper.cadastro.UsuarioPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.UsuarioRepository;

@Component 
public class UsuarioCadastroGatewayImpl implements UsuarioGateway {

  private final UsuarioRepository usuarioRepository;
  private final UsuarioPersistenceMapper usuarioPersistenceMapper;

  public UsuarioCadastroGatewayImpl(
    UsuarioRepository usuarioRepository,
    UsuarioPersistenceMapper usuarioPersistenceMapper
  ){
    this.usuarioRepository = usuarioRepository;
    this.usuarioPersistenceMapper = usuarioPersistenceMapper;
  }

  @Override
  public Usuario salvar(Usuario usuario) {
    UsuarioEntity userEntity = usuarioPersistenceMapper.toEntity(usuario);
    UsuarioEntity salvar = usuarioRepository.save(userEntity);
    return usuarioPersistenceMapper.toDomain(salvar);
  }

  @Override
  public boolean existePorEmail(String email) {
    return usuarioRepository.existsByEmailUsuarioIgnoreCase(email);
  }

  @Override
  public boolean existePorCpf(String cpf) {
    return usuarioRepository.existsByCpf(cpf);
  }

}
