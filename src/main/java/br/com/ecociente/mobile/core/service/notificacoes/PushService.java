package br.com.ecociente.mobile.core.service.notificacoes;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PushService {

    private final JdbcTemplate jdbc;

    @Transactional
    public void registrarDispositivo(int usuarioId, String instalacaoId, String token, String preferenciasJson) {
        if (instalacaoId == null || !instalacaoId.matches("[0-9a-fA-F-]{36}")
                || token == null || token.isBlank() || token.length() > 1024) {
            throw new IllegalArgumentException("Identificador de instalação ou token FCM inválido");
        }
        int atualizadas = jdbc.update("""
                INSERT INTO tb_push_dispositivos
                    (instalacao_id, usuario_id, token_fcm, preferencias, ativo, ultimo_acesso_em, atualizado_em)
                SELECT ?, u.id_usuario, ?, CAST(? AS jsonb), TRUE, now(), now()
                FROM tb_usuarios u WHERE u.id_usuario = ? AND u.ativo = TRUE
                ON CONFLICT (instalacao_id) DO UPDATE SET
                    usuario_id = EXCLUDED.usuario_id,
                    token_fcm = EXCLUDED.token_fcm,
                    preferencias = EXCLUDED.preferencias,
                    ativo = TRUE,
                    ultimo_acesso_em = now(),
                    atualizado_em = now()
                """, instalacaoId, token, preferenciasJson, usuarioId);
        if (atualizadas != 1) {
            throw new IllegalArgumentException("Usuário inativo ou não encontrado");
        }
    }

    public void desativarDispositivo(int usuarioId, String instalacaoId) {
        jdbc.update("""
                UPDATE tb_push_dispositivos SET ativo = FALSE, atualizado_em = now()
                WHERE usuario_id = ? AND instalacao_id = ?
                """, usuarioId, instalacaoId);
    }

    @Transactional
    public boolean criar(int usuarioId, String tipo, String chave, String titulo, String corpo, String destino, String acao) {
        if (usuarioId <= 0 || tipo == null || chave == null || chave.isBlank()
                || chave.length() > 240) {
            return false;
        }
        int criadas = jdbc.update("""
                INSERT INTO tb_notificacoes
                   (usuario_id, titulo_mensagem, corpo_mensagem, tipo_notificacao, chave_evento, destino, acao, enviar_apos)
                SELECT u.id_usuario, ?, ?, ?, ?, ?, ?, now()
                FROM tb_usuarios u
                WHERE u.id_usuario = ? AND u.ativo = TRUE
                ON CONFLICT DO NOTHING
                """, titulo, corpo, tipo, chave, destino, acao, usuarioId);
        return criadas > 0;
    }

    public List<Map<String, Object>> historico(int usuarioId, int limite) {
        return jdbc.queryForList("""
                SELECT id_notificacao AS id, titulo_mensagem AS titulo, corpo_mensagem AS mensagem,
                       tipo_notificacao AS tipo, COALESCE(destino, 'home') AS destino,
                       data_envio, lida_em
                FROM tb_notificacoes
                WHERE usuario_id = ? AND chave_evento IS NOT NULL
                ORDER BY id_notificacao DESC LIMIT ?
                """, usuarioId, Math.max(1, Math.min(limite, 100)));
    }

    public void marcarLida(int usuarioId, int notificacaoId) {
        jdbc.update("""
                UPDATE tb_notificacoes SET lida_em = COALESCE(lida_em, now())
                WHERE usuario_id = ? AND id_notificacao = ? AND chave_evento IS NOT NULL
                """, usuarioId, notificacaoId);
    }

    public List<Integer> usuariosComDispositivo() {
        return jdbc.queryForList("""
                SELECT DISTINCT u.id_usuario FROM tb_usuarios u
                JOIN tb_push_dispositivos d ON d.usuario_id = u.id_usuario
                WHERE u.ativo = TRUE AND d.ativo = TRUE
                """, Integer.class);
    }

    public List<Integer> buscarUsuarios(String sql, Object... parametros) {
        return jdbc.queryForList(sql, Integer.class, parametros);
    }

    public void invalidarDispositivo(String instalacaoId) {
        jdbc.update("UPDATE tb_push_dispositivos SET ativo = FALSE WHERE instalacao_id = ?", instalacaoId);
    }
}
