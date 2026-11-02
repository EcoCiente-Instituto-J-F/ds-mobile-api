package br.com.ecociente.mobile.core.service.notificacoes;

import java.util.List;
import java.util.Map;

/** Contrato da aplicacao: persistencia e Firebase ficam fora da camada core. */
public interface PushService {
    void registrarDispositivo(int usuarioId, String instalacaoId, String token, String preferenciasJson);
    void desativarDispositivo(int usuarioId, String instalacaoId);
    boolean criar(int usuarioId, String tipo, String chave, String titulo, String corpo, String destino, String acao);
    List<Map<String, Object>> historico(int usuarioId, int limite);
    void marcarLida(int usuarioId, int notificacaoId);
    List<Integer> usuariosComDispositivo();
    List<Integer> buscarUsuarios(String sql, Object... parametros);
    void invalidarDispositivo(String instalacaoId);
}
