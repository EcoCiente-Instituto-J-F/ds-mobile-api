package br.com.ecociente.mobile.dataprovider.repository;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioPerfilRepository {

  private final JdbcTemplate jdbc;

  public UsuarioPerfilRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<String> buscarPorUsuarioId(Integer usuarioId) {
    return jdbc.query(
        SQL,
        (rs, rowNum) -> rs.getString("perfil"),
        usuarioId).stream().findFirst();
  }

  private static final String SQL = """
      WITH enderecos AS (
          SELECT
              id_endereco,
              jsonb_build_object(
                  'id', id_endereco,
                  'cep', cep,
                  'estado', estado,
                  'cidade', cidade,
                  'logradouro', logradouro,
                  'numero', numero,
                  'complemento', complemento
              ) AS dados
          FROM tb_enderecos
      )
      SELECT jsonb_build_object(
          'id', u.id_usuario,
          'nome', u.nome_usuario,
          'email', u.email_usuario,
          'dataNascimento', u.data_nascimento,
          'cpf', u.cpf,
          'urlAvatar', u.url_avatar,
          'ativo', u.ativo,
          'registroEm', u.registro_em,
          'tipoUsuarioId', tu.id_tipo_usuario,
          'tipoUsuario', tu.nome_tipo,
          'moradorId', m.id_morador,
          'sindicoId', s.id_sindico,
          'endereco', eu.dados,
          'telefones', COALESCE((
              SELECT jsonb_agg(
                  jsonb_build_object(
                      'id', t.id_telefone,
                      'numero', t.numero_contato,
                      'tipo', t.tipo_telefone,
                      'ativo', t.ativo
                  )
                  ORDER BY t.id_telefone
              )
              FROM tb_telefones t
              WHERE t.usuario_id = u.id_usuario
                AND t.ativo = TRUE
          ), '[]'::jsonb),
          'cooperativa', (
              SELECT jsonb_build_object(
                  'id', coop.id_cooperativa,
                  'nome', coop.nome_cooperativa,
                  'cnpj', coop.cnpj_cooperativa,
                  'email', coop.email_cooperativa,
                  'telefone', coop.telefone_cooperativa,
                  'dataCadastro', coop.data_cadastro,
                  'endereco', ec.dados
              )
              FROM tb_cooperativas coop
              LEFT JOIN enderecos ec
                  ON ec.id_endereco = coop.endereco_id
              WHERE coop.usuario_id = u.id_usuario
          ),
          'condominios', COALESCE((
              SELECT jsonb_agg(
                  jsonb_build_object(
                      'id', c.id_condominio,
                      'nome', c.nome_condominio,
                      'cnpj', c.cnpj,
                      'ativo', c.ativo,
                      'tipoCondominioId', tc.id_tipo_condominio,
                      'tipoCondominio', tc.nome_tipo,
                      'sindicoResponsavel',
                          COALESCE(c.sindico_id = s.id_sindico, FALSE),
                      'cadastroMorador',
                          COALESCE(m.condominio_id = c.id_condominio, FALSE),
                      'endereco', ec.dados,
                      'vinculo',
                          CASE
                              WHEN v.id_usuario_condominio IS NULL THEN NULL
                              ELSE jsonb_build_object(
                                  'id', v.id_usuario_condominio,
                                  'aprovado', v.aprovado,
                                  'dataEntrada', v.data_entrada,
                                  'dataSaida', v.data_saida,
                                  'nivelConfiancaId', v.nivel_confianca_id,
                                  'nivelConfianca', nc.nome_nivel,
                                  'trustScore', v.trust_score,
                                  'postagensValidadasSemContestacao',
                                      v.postagens_validadas_sem_contestacao,
                                  'denunciasRealizadas',
                                      v.denuncias_realizadas,
                                  'denunciasProcedentes',
                                      v.denuncias_procedentes,
                                  'taxaAcertoDenuncias',
                                      v.taxa_acerto_denuncias
                              )
                          END,
                      'torres', COALESCE((
                          SELECT jsonb_agg(
                              jsonb_build_object(
                                  'id', torre.id_torre,
                                  'nome', torre.nome_torre
                              )
                              ORDER BY torre.nome_torre, torre.id_torre
                          )
                          FROM tb_torres torre
                          WHERE torre.condominio_id = c.id_condominio
                      ), '[]'::jsonb)
                  )
                  ORDER BY c.nome_condominio, c.id_condominio
              )
              FROM tb_condominios c
              JOIN tb_lkp_tipos_condominios tc
                  ON tc.id_tipo_condominio = c.tipo_condominio_id
              LEFT JOIN enderecos ec
                  ON ec.id_endereco = c.endereco_id
              LEFT JOIN tb_rel_usuarios_condominios v
                  ON v.condominio_id = c.id_condominio
                 AND v.usuario_id = u.id_usuario
              LEFT JOIN tb_lkp_niveis_confianca nc
                  ON nc.id_nivel_confianca = v.nivel_confianca_id
              WHERE c.id_condominio = m.condominio_id
                 OR c.sindico_id = s.id_sindico
                 OR v.id_usuario_condominio IS NOT NULL
          ), '[]'::jsonb)
      )::text AS perfil
      FROM tb_usuarios u
      JOIN tb_lkp_tipos_usuarios tu
          ON tu.id_tipo_usuario = u.tipo_usuario_id
      LEFT JOIN enderecos eu
          ON eu.id_endereco = u.endereco_id
      LEFT JOIN tb_moradores m
          ON m.usuario_id = u.id_usuario
      LEFT JOIN tb_sindicos s
          ON s.usuario_id = u.id_usuario
      WHERE u.id_usuario = ?
      """;
}