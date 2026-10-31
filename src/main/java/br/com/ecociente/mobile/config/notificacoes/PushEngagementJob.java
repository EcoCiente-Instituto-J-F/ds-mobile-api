package br.com.ecociente.mobile.config.notificacoes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.service.notificacoes.PushService;

@Component
public class PushEngagementJob {
    private final PushService push;
    private final boolean enabled;

    public PushEngagementJob(PushService push, @Value("${app.push.enabled:false}") boolean enabled) {
        this.push = push;
        this.enabled = enabled;
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "America/Sao_Paulo")
    public void executar() {
        if (!enabled) return;
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        int dia = hoje.getDayOfWeek().getValue();
        if (dia == 1) criarParaAtivos("dica", "Você sabia? 🌱",
                "Pequenas escolhas reduzem o desperdício. Que tal separar os recicláveis hoje?", "home", hoje.toString());
        if (dia == 2) criarParaAtivos("quiz", "Bora de desafio? 👀",
                "Tem quizzes do EcoCiente esperando por você!", "quiz", hoje.toString());
        if (dia == 3) criarParaAtivos("aulas", "Seu momento de aprender 📚",
                "Que tal continuar um curso sustentável hoje?", "home", hoje.toString());
        if (dia == 4) criarParaAtivos("motivacional", "O Eko acredita em você 💚",
                "Uma atitude sustentável já faz diferença!", "home", hoje.toString());
        if (dia == 5) criarParaAtivos("conteudos", "Uma novidade para explorar 🌱",
                "Confira os cursos e conteúdos educativos do EcoCiente.", "home", hoje.toString());
        if (dia == 6) criarParaAtivos("calendario", "Organize suas coletas ♻️",
                "Confira os seus agendamentos da próxima semana.", "calendario", hoje.toString());
        notificarRevisoes(hoje);
        notificarRetorno(hoje);
        notificarColetas(hoje);
        notificarSazonal(hoje);
    }

    private void criarParaAtivos(String tipo, String titulo, String corpo, String destino, String chave) {
        for (Integer usuarioId : push.usuariosComDispositivo()) {
            push.criar(usuarioId, tipo, "engajamento:"+tipo+":"+chave, titulo, corpo, destino, null);
        }
    }

    private void notificarRevisoes(LocalDate hoje) {
        if (hoje.getDayOfWeek().getValue() != 7) return;
        List<Integer> usuarios = push.buscarUsuarios("""
                SELECT DISTINCT t.usuario_id FROM tb_tentativas_quiz t
                JOIN tb_push_dispositivos d ON d.usuario_id = t.usuario_id AND d.ativo
                WHERE t.aprovado = FALSE AND t.concluido_em IS NOT NULL
                """);
        for (Integer id : usuarios) {
            push.criar(id, "revisao", "revisao:"+hoje, "Que tal tentar de novo? 📖",
                    "Seu próximo quiz pode ser uma nova conquista!", "quiz", "Tentar novamente");
        }
    }

    private void notificarRetorno(LocalDate hoje) {
        if (hoje.getDayOfWeek().getValue() != 7) return;
        List<Integer> usuarios = push.buscarUsuarios("""
                SELECT d.usuario_id FROM tb_push_dispositivos d
                JOIN tb_usuarios u ON u.id_usuario = d.usuario_id AND u.ativo
                GROUP BY d.usuario_id
                HAVING MAX(d.ultimo_acesso_em) < now() - interval '7 days'
                """);
        for (Integer id : usuarios) {
            push.criar(id, "retorno", "retorno:"+hoje, "Eko sentiu sua falta 👀",
                    "Tem novidades sustentáveis esperando por você!", "home", null);
        }
    }

    private void notificarColetas(LocalDate hoje) {
        List<Integer> usuarios = push.buscarUsuarios("""
                SELECT DISTINCT c.solicitado_por_usuario_id
                FROM tb_agendamentos_coletas c
                WHERE c.solicitado_por_usuario_id IS NOT NULL
                  AND c.status_agendamento_id IN (2, 12)
                  AND (c.data_inicio AT TIME ZONE 'America/Sao_Paulo')::date = ?
                """, java.sql.Date.valueOf(hoje.plusDays(1)));
        for (Integer id : usuarios) {
            push.criar(id, "coleta_amanha", "coleta_amanha:"+hoje, "A coleta é amanhã ♻️",
                    "Já deixe os recicláveis separados!", "calendario", "Ver coleta");
        }
        usuarios = push.buscarUsuarios("""
                SELECT DISTINCT c.solicitado_por_usuario_id
                FROM tb_agendamentos_coletas c
                WHERE c.solicitado_por_usuario_id IS NOT NULL
                  AND c.status_agendamento_id IN (2, 12)
                  AND (c.data_inicio AT TIME ZONE 'America/Sao_Paulo')::date = ?
                """, java.sql.Date.valueOf(hoje));
        for (Integer id : usuarios) {
            push.criar(id, "coleta_hoje", "coleta_hoje:"+hoje, "Hoje é dia de coleta! 🚛",
                    "Confira o horário da sua coleta.", "calendario", null);
        }
    }

    private void notificarSazonal(LocalDate hoje) {
        if (!("06-05".equals(hoje.toString().substring(5))
                || "03-22".equals(hoje.toString().substring(5)))) return;
        criarParaAtivos("sazonal", "Dia de cuidar do nosso planeta 🌎",
                "Uma escolha consciente faz a diferença!", "home", hoje.toString());
    }
}
