package br.com.ecociente.mobile.dataprovider.gateway.cadastro;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.ecociente.mobile.core.domain.cadastro.AtualizacaoCondominio;
import br.com.ecociente.mobile.core.domain.cadastro.AtualizacaoDados;
import br.com.ecociente.mobile.core.domain.cadastro.Endereco;
import br.com.ecociente.mobile.core.domain.cadastro.UsuarioEdicao;
import br.com.ecociente.mobile.core.gateway.cadastro.AtualizacaoUsuarioGateway;

@Component
public class AtualizacaoUsuarioGatewayImpl
        implements AtualizacaoUsuarioGateway {

    private final JdbcTemplate jdbc;

    public AtualizacaoUsuarioGatewayImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UsuarioEdicao> buscarUsuarioAtivo(
        Integer usuarioId
    ) {
        return jdbc.query(
            """
            SELECT u.id_usuario, t.nome_tipo, u.endereco_id,
                   u.data_nascimento
              FROM tb_usuarios u
              JOIN tb_lkp_tipos_usuarios t
                ON t.id_tipo_usuario = u.tipo_usuario_id
             WHERE u.id_usuario = ?
               AND u.ativo = TRUE
             FOR UPDATE OF u
            """,
            (rs, row) -> {
                Date nascimento = rs.getDate("data_nascimento");

                return new UsuarioEdicao(
                    rs.getInt("id_usuario"),
                    rs.getString("nome_tipo"),
                    rs.getObject("endereco_id", Integer.class),
                    nascimento == null
                        ? null
                        : nascimento.toLocalDate()
                );
            },
            usuarioId
        ).stream().findFirst();
    }

    @Override
    public boolean emailPertenceAOutroUsuario(
        String email,
        Integer usuarioId
    ) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1
                  FROM tb_usuarios
                 WHERE lower(trim(email_usuario)) = ?
                   AND id_usuario <> ?
            )
            """,
            Boolean.class,
            email,
            usuarioId
        ));
    }

    @Override
    public void atualizarDados(
        Integer usuarioId,
        AtualizacaoDados dados
    ) {
        jdbc.update(
            """
            UPDATE tb_usuarios
               SET nome_usuario = COALESCE(?, nome_usuario),
                   email_usuario = COALESCE(?, email_usuario),
                   data_nascimento = COALESCE(
                       CAST(? AS DATE), data_nascimento
                   ),
                   url_avatar = COALESCE(?, url_avatar)
             WHERE id_usuario = ?
            """,
            dados.nome(),
            dados.email(),
            dados.dataNascimento() == null
                ? null
                : Date.valueOf(dados.dataNascimento()),
            dados.urlAvatar(),
            usuarioId
        );
    }

    @Override
    public List<Integer> buscarTelefonesAtivos(Integer usuarioId) {
        return jdbc.query(
            """
            SELECT id_telefone
              FROM tb_telefones
             WHERE usuario_id = ?
               AND ativo = TRUE
             ORDER BY id_telefone
            """,
            (rs, row) -> rs.getInt("id_telefone"),
            usuarioId
        );
    }

    @Override
    public void criarTelefone(Integer usuarioId, String numero) {
        jdbc.update(
            """
            INSERT INTO tb_telefones (
                usuario_id, numero_contato, ativo
            ) VALUES (?, ?, TRUE)
            """,
            usuarioId,
            numero
        );
    }

    @Override
    public void atualizarTelefone(
        Integer usuarioId,
        Integer telefoneId,
        String numero
    ) {
        jdbc.update(
            """
            UPDATE tb_telefones
               SET numero_contato = ?
             WHERE id_telefone = ?
               AND usuario_id = ?
               AND ativo = TRUE
            """,
            numero,
            telefoneId,
            usuarioId
        );
    }

    @Override
    public Optional<Integer> buscarEnderecoCooperativa(
        Integer usuarioId
    ) {
        return jdbc.query(
            """
            SELECT endereco_id
              FROM tb_cooperativas
             WHERE usuario_id = ?
               AND endereco_id IS NOT NULL
             FOR UPDATE
            """,
            (rs, row) -> rs.getInt("endereco_id"),
            usuarioId
        ).stream().findFirst();
    }

    @Override
    public boolean enderecoExclusivoDoUsuario(
        Integer usuarioId,
        Integer enderecoId
    ) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            """
            SELECT
                NOT EXISTS (
                    SELECT 1 FROM tb_usuarios
                     WHERE endereco_id = ?
                       AND id_usuario <> ?
                )
                AND NOT EXISTS (
                    SELECT 1 FROM tb_condominios
                     WHERE endereco_id = ?
                )
                AND NOT EXISTS (
                    SELECT 1 FROM tb_cooperativas
                     WHERE endereco_id = ?
                )
                AND NOT EXISTS (
                    SELECT 1 FROM tb_pontos_coletas
                     WHERE endereco_id = ?
                )
            """,
            Boolean.class,
            enderecoId,
            usuarioId,
            enderecoId,
            enderecoId,
            enderecoId
        ));
    }

    @Override
    public Optional<Integer> buscarEnderecoCondominioDoSindico(
        Integer usuarioId,
        Integer condominioId
    ) {
        return jdbc.query(
            """
            SELECT c.endereco_id
              FROM tb_condominios c
              JOIN tb_sindicos s
                ON s.id_sindico = c.sindico_id
             WHERE c.id_condominio = ?
               AND s.usuario_id = ?
               AND c.ativo = TRUE
               AND c.endereco_id IS NOT NULL
             FOR UPDATE OF c
            """,
            (rs, row) -> rs.getInt("endereco_id"),
            condominioId,
            usuarioId
        ).stream().findFirst();
    }

    @Override
    public void atualizarEndereco(Endereco endereco) {
        jdbc.update(
            """
            UPDATE tb_enderecos
               SET cep = ?,
                   estado = ?,
                   cidade = ?,
                   logradouro = ?,
                   numero = ?,
                   complemento = ?
             WHERE id_endereco = ?
            """,
            endereco.getCep(),
            endereco.getEstado(),
            endereco.getCidade(),
            endereco.getLogradouro(),
            endereco.getNumero(),
            endereco.getComplemento(),
            endereco.getIdEndereco()
        );
    }

    @Override
    public boolean condominioAtivoDoSindico(
        Integer usuarioId,
        Integer condominioId
    ) {
        return !jdbc.query(
            """
            SELECT c.id_condominio
              FROM tb_condominios c
              JOIN tb_sindicos s
                ON s.id_sindico = c.sindico_id
             WHERE c.id_condominio = ?
               AND s.usuario_id = ?
               AND c.ativo = TRUE
             FOR UPDATE OF c
            """,
            (rs, row) -> rs.getInt("id_condominio"),
            condominioId,
            usuarioId
        ).isEmpty();
    }

    @Override
    public boolean tipoCondominioExiste(Integer tipoCondominioId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1
                  FROM tb_lkp_tipos_condominios
                 WHERE id_tipo_condominio = ?
            )
            """,
            Boolean.class,
            tipoCondominioId
        ));
    }

    @Override
    public void atualizarCondominio(
        Integer condominioId,
        AtualizacaoCondominio dados
    ) {
        if (dados.urlFoto() == null) {
            jdbc.update(
                """
                UPDATE tb_condominios
                   SET nome_condominio = COALESCE(?, nome_condominio),
                       tipo_condominio_id = COALESCE(?, tipo_condominio_id)
                 WHERE id_condominio = ?
                """,
                dados.nome(),
                dados.tipoCondominioId(),
                condominioId
            );

            return;
        }

        Boolean possuiColuna = jdbc.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1
                  FROM pg_attribute
                 WHERE attrelid = to_regclass('tb_condominios')
                   AND attname = 'url_foto'
                   AND attnum > 0
                   AND NOT attisdropped
            )
            """,
            Boolean.class
        );

        if (!Boolean.TRUE.equals(possuiColuna)) {
            throw new br.com.ecociente.mobile.core.exception.cadastro.BusinessException(
                "FIELD_NOT_SUPPORTED",
                "O banco atual não possui foto do condomínio"
            );
        }

        jdbc.update(
            """
            UPDATE tb_condominios
               SET nome_condominio = COALESCE(?, nome_condominio),
                   tipo_condominio_id = COALESCE(?, tipo_condominio_id),
                   url_foto = ?
             WHERE id_condominio = ?
            """,
            dados.nome(),
            dados.tipoCondominioId(),
            dados.urlFoto(),
            condominioId
        );
    }


    @Override
    public void desativarUsuario(Integer usuarioId) {
        jdbc.update("UPDATE tb_usuarios SET ativo = FALSE WHERE id_usuario = ?", usuarioId);
    }
}