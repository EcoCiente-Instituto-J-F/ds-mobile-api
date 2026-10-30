package br.com.ecociente.mobile.dataprovider.gateway.autenticacao;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.autenticacao.Usuario;
import br.com.ecociente.mobile.core.gateway.autenticacao.UsuarioGateway;
import br.com.ecociente.mobile.dataprovider.mapper.autenticacao.UsuarioAutenticacaoPersistenceMapper;
import br.com.ecociente.mobile.dataprovider.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioAutenticacaoGatewayImpl
    implements UsuarioGateway {

  private final UsuarioRepository usuarioRepository;
  private final UsuarioAutenticacaoPersistenceMapper mapper;

  @Override
  public Optional<Usuario> buscarPorEmail(String email) {
    return usuarioRepository
        .findByEmailUsuarioIgnoreCase(email)
        .map(mapper::toDomain);
  }

  @Override
  public boolean existePorEmail(String email) {
    return usuarioRepository
        .existsByEmailUsuarioIgnoreCase(email);
  }
}