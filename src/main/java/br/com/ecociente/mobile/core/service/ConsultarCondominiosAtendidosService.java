package br.com.ecociente.mobile.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import br.com.ecociente.mobile.core.domain.PaginaCondominiosAtendidos;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException;
import br.com.ecociente.mobile.core.exception.ConsultaCooperativaException.Tipo;
import br.com.ecociente.mobile.core.gateway.AvaliacaoCooperativaGateway;
import br.com.ecociente.mobile.core.gateway.CondominioAtendidoGateway;

@Service
public class ConsultarCondominiosAtendidosService {
  private final AvaliacaoCooperativaGateway cooperativaGateway;
  private final CondominioAtendidoGateway gateway;

  public ConsultarCondominiosAtendidosService(AvaliacaoCooperativaGateway cooperativaGateway,
      CondominioAtendidoGateway gateway) {
    this.cooperativaGateway = cooperativaGateway;
    this.gateway = gateway;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public PaginaCondominiosAtendidos executar(Integer usuarioId, Integer cooperativaId,
      int pagina, int tamanho) {
    if (usuarioId == null || usuarioId <= 0) {
      throw new ConsultaCooperativaException(Tipo.NAO_PERMITIDO, "USUARIO_INVALIDO", "Usuário inválido");
    }
    if (cooperativaId == null || cooperativaId <= 0 || pagina < 0 || tamanho < 1 || tamanho > 100) {
      throw new ConsultaCooperativaException(Tipo.DADOS_INVALIDOS, "CONSULTA_INVALIDA",
          "Informe cooperativa positiva, página a partir de zero e tamanho entre 1 e 100");
    }
    var responsavel = cooperativaGateway.buscarResponsavelAtivo(cooperativaId)
        .orElseThrow(() -> new ConsultaCooperativaException(Tipo.NAO_ENCONTRADO,
            "COOPERATIVA_NAO_ENCONTRADA", "Cooperativa não encontrada ou sem responsável ativo"));
    if (!usuarioId.equals(responsavel)) {
      throw new ConsultaCooperativaException(Tipo.NAO_PERMITIDO, "CONDOMINIOS_NAO_PERMITIDOS",
          "Você não pode consultar os condomínios desta cooperativa");
    }
    long total = gateway.contar(cooperativaId);
    return new PaginaCondominiosAtendidos(cooperativaId, total, pagina, tamanho,
        total / tamanho + (total % tamanho == 0 ? 0 : 1),
        gateway.listar(cooperativaId, (long) pagina * tamanho, tamanho));
  }
}
