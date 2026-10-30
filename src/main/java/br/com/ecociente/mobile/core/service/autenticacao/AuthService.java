package br.com.ecociente.mobile.core.service.autenticacao;

import org.springframework.stereotype.Service;

import br.com.ecociente.mobile.core.domain.autenticacao.Autenticacao;
import br.com.ecociente.mobile.core.domain.autenticacao.PerfilUsuarioType;
import br.com.ecociente.mobile.core.domain.autenticacao.SessaoAutenticada;
import br.com.ecociente.mobile.core.domain.autenticacao.Usuario;
import br.com.ecociente.mobile.core.domain.autenticacao.UsuarioCredenciais;
import br.com.ecociente.mobile.core.exception.autenticacao.RecursoNaoEncontradoException;
import br.com.ecociente.mobile.core.gateway.autenticacao.AutenticacaoGateway;
import br.com.ecociente.mobile.core.gateway.autenticacao.AutenticadorCredenciaisPort;
import br.com.ecociente.mobile.core.gateway.autenticacao.TokenProvaiderPort;
import br.com.ecociente.mobile.core.gateway.autenticacao.UsuarioGateway;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AutenticadorCredenciaisPort autenticadorCredenciaisPort;
  private final TokenProvaiderPort tokenProvaiderPort;
  private final ResolverPerfilAutenticacaoService usuarioPerfilService;
  private final UsuarioGateway usuarioGateway;
  private final AutenticacaoGateway autenticacaoGateway;
  
  public SessaoAutenticada login(UsuarioCredenciais credenciais){
    autenticadorCredenciaisPort.autenticar(credenciais.getEmail(),credenciais.getSenha());

      Usuario usuario = usuarioGateway.buscarPorEmail(credenciais.getEmail())
      .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
      PerfilUsuarioType perfil = usuarioPerfilService.perfil(usuario);

      String token = tokenProvaiderPort.gerarToken(usuario, perfil);
      int expiracaoSegundos = Math.toIntExact(tokenProvaiderPort.getExpiracaoSegundos());
      autenticacaoGateway.salvar(new Autenticacao(
        null,
        usuario.getId(),
        token,
        "Bearer",
        null,
        expiracaoSegundos
      ));

      return new SessaoAutenticada(
        token,
        "Bearer",
        expiracaoSegundos,
        usuario.getId(),
        usuario.getNome(),
        usuario.getEmail(),
        perfil);
  }
  
}
