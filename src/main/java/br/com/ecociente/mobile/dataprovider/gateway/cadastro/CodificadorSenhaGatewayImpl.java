package br.com.ecociente.mobile.dataprovider.gateway.cadastro;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.gateway.cadastro.CodificadorSenhaGateway;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CodificadorSenhaGatewayImpl
    implements CodificadorSenhaGateway {

  private final PasswordEncoder passwordEncoder;

  @Override
  public String gerarHash(String senha) {
    return passwordEncoder.encode(senha);
  }
}