package br.com.ecociente.mobile.core.gateway.cadastro;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.Condominio;
import br.com.ecociente.mobile.core.domain.cadastro.Morador;

@Component 
public interface CondominioGateway {
  Optional<Condominio> buscarPorCodigoAcesso(String codigoAcesso);
  Morador vincularMorador(Morador morador);
}
