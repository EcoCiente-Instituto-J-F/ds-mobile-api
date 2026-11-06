package br.com.ecociente.mobile.core.gateway;

import java.util.List;
import br.com.ecociente.mobile.core.domain.Cooperativa;

public interface CooperativaGateway {

    boolean sindicoTemCondominioAtivo(Integer usuarioId);

    List<Integer> listarCondominiosAtivos(Integer usuarioId);

    long contarProximas(Integer usuarioId);

    List<Cooperativa> listarProximas(Integer usuarioId, long deslocamento, int limite);
}
