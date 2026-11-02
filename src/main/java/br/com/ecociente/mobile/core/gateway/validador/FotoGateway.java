package br.com.ecociente.mobile.core.gateway.validador;

import br.com.ecociente.mobile.core.domain.validador.FotoCarregada;

public interface FotoGateway {

    FotoCarregada baixar(String url);
}
