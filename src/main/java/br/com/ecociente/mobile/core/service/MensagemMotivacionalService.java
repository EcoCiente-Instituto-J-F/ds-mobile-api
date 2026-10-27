package br.com.ecociente.mobile.core.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MensagemMotivacionalService {

    private static final String FILTRO = """
            FROM tb_notificacoes
            WHERE LOWER(TRIM(tipo_notificacao)) = 'motivacional'
              AND corpo_mensagem IS NOT NULL
              AND BTRIM(corpo_mensagem) <> ''
            """;

    private final JdbcTemplate jdbc;

    public MensagemMotivacionalService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record MensagemMotivacional(Long id, String mensagem) {}

    public Optional<MensagemMotivacional> sortear(Long excluirId) {
        boolean excluir = excluirId != null && excluirId > 0;
        String clausulaExclusao = excluir ? " AND id_notificacao <> ?" : "";
        Object[] argumentos = excluir ? new Object[]{excluirId} : new Object[0];

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) " + FILTRO + clausulaExclusao,
                Long.class,
                argumentos);

        if (total == null || total == 0) {
            if (excluir) {
                return sortear(null);
            }
            return Optional.empty();
        }

        int offset = ThreadLocalRandom.current().nextInt(Math.toIntExact(total));
        String sql = """
                SELECT id_notificacao, corpo_mensagem
                """ + FILTRO + clausulaExclusao
                + " ORDER BY id_notificacao LIMIT 1 OFFSET ?";

        Object[] parametros = excluir
                ? new Object[]{excluirId, offset}
                : new Object[]{offset};

        List<MensagemMotivacional> mensagens = jdbc.query(
                sql,
                (rs, index) -> new MensagemMotivacional(
                        rs.getLong("id_notificacao"),
                        rs.getString("corpo_mensagem").trim()),
                parametros);

        return mensagens.stream().findFirst();
    }
}
