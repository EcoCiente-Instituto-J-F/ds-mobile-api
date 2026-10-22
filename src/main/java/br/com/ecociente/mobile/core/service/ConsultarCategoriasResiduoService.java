package br.com.ecociente.mobile.core.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ecociente.mobile.core.domain.CategoriaResiduo;
import br.com.ecociente.mobile.core.gateway.PostagemGateway;

/** Tipos de resíduo disponíveis para o morador escolher ao registrar um descarte. */
@Service
public class ConsultarCategoriasResiduoService {

  private final PostagemGateway gateway;

  public ConsultarCategoriasResiduoService(PostagemGateway gateway) {
    this.gateway = gateway;
  }

  @Transactional(readOnly = true)
  public List<CategoriaResiduo> executar() {
    return gateway.listarCategorias();
  }
}
