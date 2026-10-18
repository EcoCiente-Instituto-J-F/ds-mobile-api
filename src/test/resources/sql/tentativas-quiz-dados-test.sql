INSERT INTO tb_lkp_tipos_usuarios (
    id_tipo_usuario, nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Usuário comum'),
    (2, 'Morador'),
    (3, 'Síndico');

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

INSERT INTO tb_enderecos (
    id_endereco, cep, estado, cidade, logradouro, numero
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '01001000', 'SP', 'São Paulo', 'Rua Um', '10'),
    (2, '01001000', 'SP', 'São Paulo', 'Rua Dois', '20');

INSERT INTO tb_usuarios (
    id_usuario, nome_usuario, email_usuario,
    senha_hash, ativo, tipo_usuario_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (10, 'Comum', 'comum@teste.com', 'hash', TRUE, 1, 1),
    (20, 'Morador', 'morador@teste.com', 'hash', TRUE, 2, 1),
    (30, 'Outro usuário', 'outro@teste.com', 'hash', TRUE, 1, 2),
    (40, 'Síndico', 'sindico@teste.com', 'hash', TRUE, 3, 2);

INSERT INTO tb_condominios (
    id_condominio, nome_condominio,
    ativo, tipo_condominio_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Condomínio Um', TRUE, 1, 1),
    (2, 'Condomínio Dois', TRUE, 1, 2);

INSERT INTO tb_torres (
    id_torre, nome_torre, condominio_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Torre Um', 1),
    (2, 'Torre Dois', 2);

INSERT INTO tb_moradores (
    id_morador, condominio_id, usuario_id, torre_id
)
OVERRIDING SYSTEM VALUE
VALUES (1, 1, 20, 1);

INSERT INTO tb_rel_usuarios_condominios (
    usuario_id,
    condominio_id,
    aprovado,
    nivel_confianca_id,
    trust_score,
    postagens_validadas_sem_contestacao,
    denuncias_realizadas,
    denuncias_procedentes
)
VALUES (20, 1, TRUE, 1, 0, 0, 0, 0);

INSERT INTO tb_cursos (
    id_curso, titulo_curso, descricao_curso, esta_ativo
)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Reciclagem', 'Curso de teste', TRUE);

INSERT INTO tb_aulas (
    id_aula, curso_id, titulo_aula, conteudo_aula, ordem
)
OVERRIDING SYSTEM VALUE
VALUES (10, 1, 'Aula Um', 'Conteúdo', 1);

INSERT INTO tb_quizzes (
    id_quiz, curso_id, aula_id, titulo_quiz,
    nota_minima_aprovacao, pontos_recompensa, ativo
)
OVERRIDING SYSTEM VALUE
VALUES (100, 1, 10, 'Quiz Um', 70, 30, TRUE);

INSERT INTO tb_perguntas_quiz (
    id_pergunta, quiz_id, enunciado, ordem
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 100, 'Pergunta um?', 1),
    (2, 100, 'Pergunta dois?', 2);

INSERT INTO tb_alternativas_quiz (
    id_alternativa, pergunta_id, texto_alternativa, correta
)
OVERRIDING SYSTEM VALUE
VALUES
    (10, 1, 'Correta um', TRUE),
    (11, 1, 'Errada um', FALSE),
    (20, 2, 'Correta dois', TRUE),
    (21, 2, 'Errada dois', FALSE);