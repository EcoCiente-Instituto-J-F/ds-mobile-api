package br.com.ecociente.mobile.dataprovider.gateway.notificacoes;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.ecociente.mobile.core.service.notificacoes.PushService;
import br.com.ecociente.mobile.config.notificacoes.PushAudience;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JdbcPushService implements PushService {
    private static final Logger log = LoggerFactory.getLogger(JdbcPushService.class);
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactionManager;
    private final ObjectMapper mapper;
    private final PushAudience audience;

    @Value("${app.push.enabled:false}")
    private boolean enabled;

    @Override
    @Transactional
    public void registrarDispositivo(int usuarioId, String instalacaoId, String token, String preferenciasJson) {
        if (usuarioId <= 0 || instalacaoId == null
                || !instalacaoId.matches("[0-9a-fA-F-]{36}")
                || token == null || token.isBlank() || token.length() > 1024
                || preferenciasJson == null || preferenciasJson.isBlank()) {
            throw new IllegalArgumentException("Instalacao, token ou preferencias invalidos");
        }
        validarPreferencias(preferenciasJson);
        // Um token FCM pertence a uma unica instalacao ativa. Rotacao e reinstalacao nao devem gerar duplicatas.
        jdbc.update("""
                UPDATE tb_push_dispositivos SET ativo = FALSE, atualizado_em = now()
                WHERE token_fcm = ? AND instalacao_id <> ?
                """, token, instalacaoId);
        int atualizadas = jdbc.update("""
                INSERT INTO tb_push_dispositivos
                  (instalacao_id, usuario_id, token_fcm, preferencias, ativo, ultimo_acesso_em, atualizado_em)
                SELECT ?, u.id_usuario, ?, CAST(? AS jsonb), TRUE, now(), now()
                FROM tb_usuarios u WHERE u.id_usuario = ? AND u.ativo = TRUE
                ON CONFLICT (instalacao_id) DO UPDATE SET
                  usuario_id = EXCLUDED.usuario_id,
                  token_fcm = EXCLUDED.token_fcm,
                  preferencias = EXCLUDED.preferencias,
                  ativo = TRUE, ultimo_acesso_em = now(), atualizado_em = now()
                """, instalacaoId, token, preferenciasJson, usuarioId);
        if (atualizadas != 1) {
            throw new IllegalArgumentException("Usuario inativo ou nao encontrado");
        }
    }

    private void validarPreferencias(String json) {
        try {
            JsonNode preferencias = mapper.readTree(json);
            if (preferencias == null || !preferencias.isObject() || preferencias.size() != 4
                    || !preferencias.path("notificacoesGerais").isBoolean()
                    || !preferencias.path("lembretesAvisos").isBoolean()
                    || !preferencias.path("novasAtividades").isBoolean()
                    || !preferencias.path("dicasSustentaveis").isBoolean()) {
                throw new IllegalArgumentException("As quatro preferencias devem ser booleanas");
            }
        } catch (JsonProcessingException erro) {
            throw new IllegalArgumentException("Preferencias invalidas", erro);
        }
    }

    @Override
    public void desativarDispositivo(int usuarioId, String instalacaoId) {
        jdbc.update("""
                UPDATE tb_push_dispositivos SET ativo = FALSE, atualizado_em = now()
                WHERE usuario_id = ? AND instalacao_id = ?
                """, usuarioId, instalacaoId);
    }

    @Override
    public boolean criar(int usuarioId, String tipo, String chave, String titulo, String corpo, String destino, String acao) {
        if (!enabled || !audience.permitir(usuarioId) || usuarioId <= 0 || tipo == null || tipo.isBlank() || chave == null
                || chave.isBlank() || chave.length() > 240 || titulo == null || titulo.isBlank()) {
            return false;
        }
        // Nao gerar push de uma operacao que ainda possa sofrer rollback.
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    gravarComSeguranca(usuarioId, tipo, chave, titulo, corpo, destino, acao);
                }
            });
            return true;
        }
        return gravarComSeguranca(usuarioId, tipo, chave, titulo, corpo, destino, acao);
    }

    private boolean gravarComSeguranca(int usuarioId, String tipo, String chave, String titulo,
            String corpo, String destino, String acao) {
        try {
            TransactionTemplate transacao = new TransactionTemplate(transactionManager);
            transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return Boolean.TRUE.equals(transacao.execute(status -> jdbc.update("""
                    INSERT INTO tb_notificacoes
                      (usuario_id, titulo_mensagem, corpo_mensagem, tipo_notificacao, chave_evento, destino, acao, enviar_apos)
                    SELECT u.id_usuario, ?, ?, ?, ?, ?, ?, now()
                    FROM tb_usuarios u
                    WHERE u.id_usuario = ? AND u.ativo = TRUE
                      AND (? NOT IN ('solicitacao_aceita', 'solicitacao_recusada')
                           OR EXISTS (SELECT 1 FROM tb_sindicos s WHERE s.usuario_id = u.id_usuario))
                    ON CONFLICT DO NOTHING
                    """, titulo, corpo, tipo, chave, destino, acao, usuarioId, tipo) > 0));
        } catch (RuntimeException erro) {
            // Push nunca pode desfazer a coleta, o quiz ou a pontuacao.
            log.warn("Nao foi possivel persistir evento push do tipo {} para o usuario {}", tipo, usuarioId, erro);
            return false;
        }
    }

    @Override
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

    @Override
    public void marcarLida(int usuarioId, int notificacaoId) {
        jdbc.update("""
                UPDATE tb_notificacoes SET lida_em = COALESCE(lida_em, now())
                WHERE usuario_id = ? AND id_notificacao = ? AND chave_evento IS NOT NULL
                """, usuarioId, notificacaoId);
    }

    @Override
    public List<Integer> usuariosComDispositivo() {
        return jdbc.queryForList("""
                SELECT DISTINCT u.id_usuario FROM tb_usuarios u
                JOIN tb_push_dispositivos d ON d.usuario_id = u.id_usuario
                WHERE u.ativo = TRUE AND d.ativo = TRUE
                """, Integer.class).stream().filter(audience::permitir).toList();
    }

    @Override
    public List<Integer> buscarUsuarios(String sql, Object... parametros) {
        return jdbc.queryForList(sql, Integer.class, parametros).stream().filter(audience::permitir).toList();
    }

    @Override
    public void invalidarDispositivo(String instalacaoId) {
        jdbc.update("UPDATE tb_push_dispositivos SET ativo = FALSE WHERE instalacao_id = ?", instalacaoId);
    }
}
