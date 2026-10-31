package br.com.ecociente.mobile.config.notificacoes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;

import br.com.ecociente.mobile.core.service.notificacoes.PushService;

@Component
public class PushDispatchJob {
    private static final Logger log = LoggerFactory.getLogger(PushDispatchJob.class);
    private final JdbcTemplate jdbc;
    private final FirebaseMessaging firebase;
    private final ObjectMapper mapper;
    private final PushService push;
    private final boolean enabled;

    public PushDispatchJob(JdbcTemplate jdbc, FirebaseMessaging firebase, ObjectMapper mapper,
            PushService push, @Value("${app.push.enabled:false}") boolean enabled) {
        this.jdbc = jdbc;
        this.firebase = firebase;
        this.mapper = mapper;
        this.push = push;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${app.push.dispatch-delay-ms:15000}")
    public void executar() {
        if (!enabled) return;
        try {
            jdbc.update("""
                    INSERT INTO tb_push_entregas (notificacao_id, instalacao_id)
                    SELECT n.id_notificacao, d.instalacao_id
                    FROM tb_notificacoes n
                    JOIN tb_push_dispositivos d ON d.usuario_id = n.usuario_id AND d.ativo = TRUE
                    WHERE n.chave_evento IS NOT NULL AND n.enviar_apos <= now()
                      AND n.data_envio >= now() - interval '48 hours'
                      AND COALESCE(d.preferencias ->> 'notificacoesGerais', 'true') = 'true'
                    ON CONFLICT DO NOTHING
                    """);
            List<Map<String, Object>> lotes = jdbc.queryForList("""
                    WITH lote AS (
                        SELECT e.id_entrega FROM tb_push_entregas e
                        WHERE e.proxima_tentativa <= now()
                          AND (e.estado IN ('PENDENTE', 'ERRO')
                            OR (e.estado = 'PROCESSANDO' AND e.bloqueado_em < now() - interval '5 minutes'))
                        ORDER BY e.id_entrega LIMIT 40 FOR UPDATE SKIP LOCKED
                    )
                    UPDATE tb_push_entregas e
                    SET estado = 'PROCESSANDO', bloqueado_em = now(), tentativas = e.tentativas + 1
                    FROM lote WHERE e.id_entrega = lote.id_entrega
                    RETURNING e.id_entrega, e.notificacao_id, e.instalacao_id
                    """);
            for (Map<String, Object> linha : lotes) {
                enviar(((Number)linha.get("id_entrega")).longValue(),
                        ((Number)linha.get("notificacao_id")).intValue(),
                        (String)linha.get("instalacao_id"));
            }
        } catch (Exception erro) {
            log.error("Falha no processador push", erro);
        }
    }

    private void enviar(long entregaId, int notificacaoId, String instalacaoId) {
        try {
            List<Map<String, Object>> registros = jdbc.queryForList("""
                    SELECT n.usuario_id, n.tipo_notificacao, n.titulo_mensagem, n.corpo_mensagem,
                           n.destino, n.acao, d.token_fcm, d.preferencias, d.ativo
                    FROM tb_notificacoes n
                    JOIN tb_push_dispositivos d ON d.usuario_id = n.usuario_id
                    WHERE n.id_notificacao = ? AND d.instalacao_id = ?
                    """, notificacaoId, instalacaoId);
            if (registros.isEmpty() || !Boolean.TRUE.equals(registros.getFirst().get("ativo"))) {
                descartar(entregaId, "Dispositivo removido");
                return;
            }
            Map<String, Object> registro = registros.getFirst();
            Map<String, Boolean> preferencias = mapper.readValue(
                    registro.get("preferencias").toString(),
                    new TypeReference<Map<String, Boolean>>() {});
            String grupo = grupo((String)registro.get("tipo_notificacao"));
            if (!preferencias.getOrDefault("notificacoesGerais", true)
                    || !preferencias.getOrDefault(grupo, true)) {
                descartar(entregaId, "Preferência desabilitada");
                return;
            }
            Map<String, String> dados = new HashMap<>();
            dados.put("idNotificacao", String.valueOf(notificacaoId));
            dados.put("usuarioId", String.valueOf(registro.get("usuario_id")));
            dados.put("tipo", String.valueOf(registro.get("tipo_notificacao")));
            dados.put("titulo", String.valueOf(registro.get("titulo_mensagem")));
            dados.put("mensagem", String.valueOf(registro.get("corpo_mensagem")));
            dados.put("destino", registro.get("destino") == null ? "home" : registro.get("destino").toString());
            if (registro.get("acao") != null) dados.put("acao", registro.get("acao").toString());

            Message mensagem = Message.builder()
                    .setToken(registro.get("token_fcm").toString())
                    .putAllData(dados)
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH)
                            .setTtl(3600_000).build())
                    .build();
            firebase.send(mensagem);
            jdbc.update("UPDATE tb_push_entregas SET estado='ENVIADO', enviado_em=now(), bloqueado_em=NULL, ultimo_erro=NULL WHERE id_entrega=?", entregaId);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode codigo = e.getMessagingErrorCode();
            if (codigo == MessagingErrorCode.UNREGISTERED || codigo == MessagingErrorCode.INVALID_ARGUMENT) {
                push.invalidarDispositivo(instalacaoId);
                descartar(entregaId, "Token FCM inválido");
            } else {
                falhar(entregaId, e.getMessage());
            }
        } catch (Exception e) {
            falhar(entregaId, e.getMessage());
        }
    }

    private void descartar(long id, String motivo) {
        jdbc.update("UPDATE tb_push_entregas SET estado='DESCARTADO', bloqueado_em=NULL, ultimo_erro=? WHERE id_entrega=?", motivo, id);
    }

    private void falhar(long id, String motivo) {
        jdbc.update("""
                UPDATE tb_push_entregas
                SET estado=CASE WHEN tentativas >= 5 THEN 'DESCARTADO' ELSE 'ERRO' END,
                    bloqueado_em=NULL, ultimo_erro=?, proxima_tentativa=now() + (LEAST(tentativas, 5)*interval '2 minutes')
                WHERE id_entrega=?
                """, motivo == null ? "Falha desconhecida" : motivo.substring(0, Math.min(400, motivo.length())), id);
    }

    static String grupo(String tipo) {
        if (tipo == null) return "novasAtividades";
        return switch (tipo) {
            case "dica", "retorno", "motivacional", "sazonal" -> "dicasSustentaveis";
            case "coleta_amanha", "coleta_hoje", "coleta_alterada", "calendario",
                 "aviso_condominio", "mensagem_nova", "solicitacao_nova",
                 "solicitacao_aceita", "solicitacao_recusada" -> "lembretesAvisos";
            default -> "novasAtividades";
        };
    }
}
