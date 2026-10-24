-- Condomínio 1: com dados. Condomínio 2: de outro síndico. 3: sem nenhum dado. 4: sem síndico.
INSERT INTO tb_lkp_tipos_usuarios (
    id_tipo_usuario, nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Síndico'),
    (2, 'Morador');

INSERT INTO tb_lkp_tipos_condominios (
    id_tipo_condominio, nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Residencial');

INSERT INTO tb_lkp_niveis_confianca (
    id_nivel_confianca, nome_nivel, peso_voto
)
OVERRIDING SYSTEM VALUE
VALUES (1, 'morador_comum', 1);

INSERT INTO tb_lkp_status_validacoes_postagens (
    id_status_validacao, nome_status
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'em_analise'),
    (2, 'aprovada'),
    (3, 'reprovada');

INSERT INTO tb_lkp_categorias_residuos (
    id_categoria, nome_categoria, cor_identificacao, pontos_base
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Plástico', '#E53935', 10),
    (2, 'Papel', '#1E88E5', 10);

INSERT INTO tb_enderecos (
    id_endereco, cep, estado, cidade, logradouro, numero
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '01001000', 'SP', 'São Paulo', 'Rua Um', '10'),
    (2, '01001000', 'SP', 'São Paulo', 'Rua Dois', '20'),
    (3, '01001000', 'SP', 'São Paulo', 'Rua Tres', '30'),
    (4, '01001000', 'SP', 'São Paulo', 'Rua Quatro', '40');

INSERT INTO tb_usuarios (
    id_usuario, nome_usuario, email_usuario,
    senha_hash, ativo, tipo_usuario_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (10, 'Síndico Um', 'sindico1@teste.com', 'hash', TRUE, 1, 1),
    (11, 'Síndico Dois', 'sindico2@teste.com', 'hash', TRUE, 1, 2),
    (20, 'Ana Morador', 'ana@teste.com', 'hash', TRUE, 2, 1),
    (30, 'Bia Vizinha', 'bia@teste.com', 'hash', TRUE, 2, 1),
    (40, 'Caio Outro Condominio', 'caio@teste.com', 'hash', TRUE, 2, 2);

INSERT INTO tb_sindicos (id_sindico, usuario_id)
OVERRIDING SYSTEM VALUE
VALUES (1, 10), (2, 11);

INSERT INTO tb_condominios (
    id_condominio, nome_condominio,
    ativo, tipo_condominio_id, sindico_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Condomínio Um', TRUE, 1, 1, 1),
    (2, 'Condomínio Dois', TRUE, 1, 2, 2),
    (3, 'Condomínio Sem Dados', TRUE, 1, 1, 3),
    (4, 'Condomínio Sem Síndico', TRUE, 1, NULL, 4);

INSERT INTO tb_moradores (
    id_morador, condominio_id, usuario_id, torre_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1, 20, NULL),
    (2, 1, 30, NULL),
    (3, 2, 40, NULL);

INSERT INTO tb_rel_usuarios_condominios (
    usuario_id, condominio_id, aprovado, nivel_confianca_id,
    trust_score, postagens_validadas_sem_contestacao,
    denuncias_realizadas, denuncias_procedentes
)
VALUES
    (20, 1, TRUE, 1, 0, 0, 0, 0),
    (30, 1, TRUE, 1, 0, 0, 0, 0),
    (40, 2, TRUE, 1, 0, 0, 0, 0);

INSERT INTO tb_cursos (
    id_curso, titulo_curso, descricao_curso, esta_ativo
)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Reciclagem', 'Curso de teste', TRUE);

INSERT INTO tb_aulas (
    id_aula, curso_id, titulo_aula, conteudo_aula, ordem
)
OVERRIDING SYSTEM VALUE
VALUES
    (10, 1, 'Aula Um', 'Conteúdo', 1),
    (11, 1, 'Aula Dois', 'Conteúdo', 2);

INSERT INTO tb_quizzes (
    id_quiz, curso_id, aula_id, titulo_quiz,
    nota_minima_aprovacao, pontos_recompensa, ativo
)
OVERRIDING SYSTEM VALUE
VALUES
    (100, 1, 10, 'Quiz Um', 70, 30, TRUE),
    (101, 1, 11, 'Quiz Dois', 70, 30, TRUE);

-- condomínio 1: 3 tentativas concluídas (notas 90, 50 e 80; 2 aprovadas) e 1 em andamento
INSERT INTO tb_tentativas_quiz (
    id_tentativa, usuario_id, quiz_id, condominio_id, nota, aprovado, concluido_em
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 20, 100, 1, 90, TRUE, now()),
    (2, 20, 100, 1, 50, FALSE, now()),
    (3, 30, 101, 1, 80, TRUE, now()),
    (4, 30, 101, 1, NULL, FALSE, NULL),
    (5, 40, 100, 2, 100, TRUE, now());

-- condomínio 1: 3 aprovadas e resolvidas (2 plástico, 1 papel), 1 em análise sem resolver,
-- 1 aprovada sem resolver, 1 em análise já resolvida e 1 reprovada
INSERT INTO tb_postagens (
    id_postagem, usuario_id, condominio_id, torre_id, categoria_id,
    url_foto, hash_foto, capturada_em, data_postagem,
    status_validacao_id, resolvido_em
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 20, 1, NULL, 1, 'https://img.teste/1.jpg', 'h1', now() - INTERVAL '3 days', now() - INTERVAL '3 days', 2, now() - INTERVAL '2 days'),
    (2, 20, 1, NULL, 1, 'https://img.teste/2.jpg', 'h2', now() - INTERVAL '3 days', now() - INTERVAL '3 days', 2, now() - INTERVAL '2 days'),
    (3, 30, 1, NULL, 2, 'https://img.teste/3.jpg', 'h3', now() - INTERVAL '3 days', now() - INTERVAL '3 days', 2, now() - INTERVAL '2 days'),
    (4, 30, 1, NULL, 2, 'https://img.teste/4.jpg', 'h4', now() - INTERVAL '1 hour', now() - INTERVAL '1 hour', 1, NULL),
    (5, 20, 1, NULL, 1, 'https://img.teste/5.jpg', 'h5', now() - INTERVAL '2 hours', now() - INTERVAL '2 hours', 2, NULL),
    (6, 30, 1, NULL, 1, 'https://img.teste/6.jpg', 'h6', now() - INTERVAL '3 days', now() - INTERVAL '3 days', 1, now() - INTERVAL '2 days'),
    (7, 20, 1, NULL, 2, 'https://img.teste/7.jpg', 'h7', now() - INTERVAL '3 days', now() - INTERVAL '3 days', 3, now() - INTERVAL '2 days'),
    (8, 40, 2, NULL, 1, 'https://img.teste/8.jpg', 'h8', now() - INTERVAL '3 days', now() - INTERVAL '3 days', 2, now() - INTERVAL '2 days');

-- pontos do condomínio 1: postagens 10 + 10 - 10 de estorno = 10; quiz 30; total 40; no mês atual 40
INSERT INTO tb_movimentacoes_pontos (
    id_movimentacao, usuario_id, condominio_id, categoria_id, origem_tipo, postagem_id,
    tentativa_quiz_id, tipo_movimentacao, pontos, movimentacao_referencia_id,
    idempotency_key, ocorrido_em
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 20, 1, 1, 'POSTAGEM', 1, NULL, 'CREDITO', 10, NULL, 'dash-1', now()),
    (2, 30, 1, 2, 'POSTAGEM', 3, NULL, 'CREDITO', 10, NULL, 'dash-2', now() - INTERVAL '70 days'),
    (3, 20, 1, NULL, 'QUIZ', NULL, 1, 'CREDITO', 30, NULL, 'dash-3', now()),
    (4, 30, 1, 2, 'POSTAGEM', 3, NULL, 'ESTORNO', 10, 2, 'dash-4', now() - INTERVAL '70 days'),
    (5, 40, 2, 1, 'POSTAGEM', 8, NULL, 'CREDITO', 99, NULL, 'dash-5', now());

CREATE OR REPLACE VIEW vw_dashboard_sindico AS
WITH moradores AS (
    SELECT m.condominio_id,
           COUNT(*)::BIGINT AS total_moradores
      FROM tb_moradores m
     GROUP BY m.condominio_id
),
postagens AS (
    SELECT p.condominio_id,
           COUNT(*) FILTER (WHERE sv.nome_status = 'aprovada'   AND p.resolvido_em IS NOT NULL)::BIGINT AS materiais_reciclados,
           COUNT(*) FILTER (WHERE p.resolvido_em IS NULL)::BIGINT                                        AS postagens_em_validacao,
           COUNT(*) FILTER (WHERE sv.nome_status = 'em_analise' AND p.resolvido_em IS NOT NULL)::BIGINT AS postagens_aguardando_sindico,
           COUNT(*) FILTER (WHERE sv.nome_status = 'reprovada'  AND p.resolvido_em IS NOT NULL)::BIGINT AS postagens_reprovadas
      FROM tb_postagens p
      JOIN tb_lkp_status_validacoes_postagens sv ON sv.id_status_validacao = p.status_validacao_id
     GROUP BY p.condominio_id
),
materiais_categoria AS (
    SELECT p.condominio_id,
           cr.id_categoria,
           cr.nome_categoria,
           cr.cor_identificacao,
           COUNT(*)::BIGINT AS quantidade
      FROM tb_postagens p
      JOIN tb_lkp_status_validacoes_postagens sv ON sv.id_status_validacao = p.status_validacao_id
      JOIN tb_lkp_categorias_residuos cr         ON cr.id_categoria = p.categoria_id
     WHERE sv.nome_status = 'aprovada'
       AND p.resolvido_em IS NOT NULL
     GROUP BY p.condominio_id, cr.id_categoria, cr.nome_categoria, cr.cor_identificacao
),
materiais_json AS (
    SELECT condominio_id,
           jsonb_agg(
               jsonb_build_object(
                   'id_categoria', id_categoria,
                   'categoria',    nome_categoria,
                   'cor',          cor_identificacao,
                   'quantidade',   quantidade
               ) ORDER BY quantidade DESC, nome_categoria
           ) AS materiais_por_categoria
      FROM materiais_categoria
     GROUP BY condominio_id
),
pontos AS (
    SELECT mp.condominio_id,
           SUM(CASE WHEN mp.tipo_movimentacao = 'ESTORNO' THEN -mp.pontos ELSE mp.pontos END)::BIGINT AS pontos_total,
           SUM(CASE WHEN mp.tipo_movimentacao = 'ESTORNO' THEN -mp.pontos ELSE mp.pontos END)
               FILTER (WHERE mp.origem_tipo = 'POSTAGEM')::BIGINT AS pontos_postagens,
           SUM(CASE WHEN mp.tipo_movimentacao = 'ESTORNO' THEN -mp.pontos ELSE mp.pontos END)
               FILTER (WHERE mp.origem_tipo = 'QUIZ')::BIGINT     AS pontos_quizzes,
           SUM(CASE WHEN mp.tipo_movimentacao = 'ESTORNO' THEN -mp.pontos ELSE mp.pontos END)
               FILTER (WHERE mp.ocorrido_em >= (date_trunc('month', now() AT TIME ZONE 'America/Sao_Paulo')
                                                AT TIME ZONE 'America/Sao_Paulo'))::BIGINT AS pontos_mes_atual
      FROM tb_movimentacoes_pontos mp
     WHERE mp.condominio_id IS NOT NULL
     GROUP BY mp.condominio_id
),
quizzes AS (
    SELECT tq.condominio_id,
           COUNT(*) FILTER (WHERE tq.concluido_em IS NOT NULL)::BIGINT                   AS tentativas_concluidas,
           COUNT(*) FILTER (WHERE tq.aprovado = TRUE)::BIGINT                            AS tentativas_aprovadas,
           COUNT(DISTINCT tq.usuario_id) FILTER (WHERE tq.concluido_em IS NOT NULL)::BIGINT AS moradores_com_quiz,
           ROUND(AVG(tq.nota) FILTER (WHERE tq.concluido_em IS NOT NULL), 2)             AS nota_media_quizzes,
           ROUND(100.0 * COUNT(*) FILTER (WHERE tq.aprovado = TRUE)
                 / NULLIF(COUNT(*) FILTER (WHERE tq.concluido_em IS NOT NULL), 0), 2)    AS taxa_aprovacao_quizzes
      FROM tb_tentativas_quiz tq
     WHERE tq.condominio_id IS NOT NULL
     GROUP BY tq.condominio_id
),
quizzes_detalhe AS (
    SELECT tq.condominio_id,
           q.id_quiz,
           q.titulo_quiz,
           COUNT(*)::BIGINT                               AS tentativas,
           COUNT(*) FILTER (WHERE tq.aprovado = TRUE)::BIGINT AS aprovadas,
           ROUND(AVG(tq.nota), 2)                         AS nota_media
      FROM tb_tentativas_quiz tq
      JOIN tb_quizzes q ON q.id_quiz = tq.quiz_id
     WHERE tq.condominio_id IS NOT NULL
       AND tq.concluido_em IS NOT NULL
     GROUP BY tq.condominio_id, q.id_quiz, q.titulo_quiz
),
quizzes_json AS (
    SELECT condominio_id,
           jsonb_agg(
               jsonb_build_object(
                   'id_quiz',    id_quiz,
                   'quiz',       titulo_quiz,
                   'tentativas', tentativas,
                   'aprovadas',  aprovadas,
                   'nota_media', nota_media
               ) ORDER BY titulo_quiz
           ) AS notas_por_quiz
      FROM quizzes_detalhe
     GROUP BY condominio_id
)
SELECT
    c.id_condominio,
    c.nome_condominio,
    s.usuario_id                                    AS sindico_usuario_id,
    us.nome_usuario                                 AS nome_sindico,
    COALESCE(mo.total_moradores, 0)                 AS total_moradores,
    -- materiais reciclados
    COALESCE(po.materiais_reciclados, 0)            AS materiais_reciclados,
    COALESCE(po.postagens_em_validacao, 0)          AS postagens_em_validacao,
    COALESCE(po.postagens_aguardando_sindico, 0)    AS postagens_aguardando_sindico,
    COALESCE(po.postagens_reprovadas, 0)            AS postagens_reprovadas,
    COALESCE(mj.materiais_por_categoria, '[]'::jsonb) AS materiais_por_categoria,
    -- pontos
    COALESCE(pt.pontos_total, 0)                    AS pontos_total,
    COALESCE(pt.pontos_postagens, 0)                AS pontos_postagens,
    COALESCE(pt.pontos_quizzes, 0)                  AS pontos_quizzes,
    COALESCE(pt.pontos_mes_atual, 0)                AS pontos_mes_atual,
    -- quizzes
    COALESCE(qz.tentativas_concluidas, 0)           AS tentativas_quiz_concluidas,
    COALESCE(qz.tentativas_aprovadas, 0)            AS tentativas_quiz_aprovadas,
    COALESCE(qz.moradores_com_quiz, 0)              AS moradores_com_quiz,
    qz.nota_media_quizzes,
    qz.taxa_aprovacao_quizzes,
    COALESCE(qj.notas_por_quiz, '[]'::jsonb)        AS notas_por_quiz
FROM tb_condominios c
LEFT JOIN tb_sindicos s      ON s.id_sindico   = c.sindico_id
LEFT JOIN tb_usuarios us     ON us.id_usuario  = s.usuario_id
LEFT JOIN moradores mo       ON mo.condominio_id = c.id_condominio
LEFT JOIN postagens po       ON po.condominio_id = c.id_condominio
LEFT JOIN materiais_json mj  ON mj.condominio_id = c.id_condominio
LEFT JOIN pontos pt          ON pt.condominio_id = c.id_condominio
LEFT JOIN quizzes qz         ON qz.condominio_id = c.id_condominio
LEFT JOIN quizzes_json qj    ON qj.condominio_id = c.id_condominio;
