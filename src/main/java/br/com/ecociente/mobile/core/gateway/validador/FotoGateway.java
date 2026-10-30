package br.com.ecociente.mobile.core.gateway.validador;

import br.com.ecociente.mobile.core.domain.validador.FotoCarregada;

/** Busca a foto no storage do app com as proteções de tamanho, host e tempo. */
public interface FotoGateway {

  FotoCarregada baixar(String url);
}
