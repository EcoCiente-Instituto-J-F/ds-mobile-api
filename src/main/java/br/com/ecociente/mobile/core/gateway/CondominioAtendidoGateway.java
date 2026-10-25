package br.com.ecociente.mobile.core.gateway;

import java.util.List;
import br.com.ecociente.mobile.core.domain.CondominioAtendido;

public interface CondominioAtendidoGateway {
  long contar(Integer cooperativaId);
  List<CondominioAtendido> listar(Integer cooperativaId, long deslocamento, int limite);
}
