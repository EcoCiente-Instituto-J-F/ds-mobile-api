INSERT INTO tb_lkp_tipos_usuarios (
    id_tipo_usuario, nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Comum'),
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
    id_categoria, nome_categoria, pontos_base
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Plástico', 10),
    (2, 'Papel', 10);

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
    (20, 'Ana Morador', 'ana@teste.com', 'hash', TRUE, 2, 1),
    (30, 'Bia Vizinha', 'bia@teste.com', 'hash', TRUE, 2, 1),
    (40, 'Caio Outro Condominio', 'caio@teste.com', 'hash', TRUE, 2, 2),
    (50, 'Davi Nao Aprovado', 'davi@teste.com', 'hash', TRUE, 2, 1);

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
VALUES
    (1, 1, 20, 1),
    (2, 1, 30, NULL),
    (3, 2, 40, 2),
    (4, 1, 50, 1);

INSERT INTO tb_rel_usuarios_condominios (
    usuario_id, condominio_id, aprovado, nivel_confianca_id,
    trust_score, postagens_validadas_sem_contestacao,
    denuncias_realizadas, denuncias_procedentes
)
VALUES
    (20, 1, TRUE, 1, 0, 0, 0, 0),
    (30, 1, TRUE, 1, 0, 0, 0, 0),
    (40, 2, TRUE, 1, 0, 0, 0, 0),
    (50, 1, FALSE, 1, 0, 0, 0, 0);

INSERT INTO tb_postagens (
    id_postagem, usuario_id, condominio_id, torre_id, categoria_id,
    url_foto, hash_foto, capturada_em, data_postagem
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 30, 1, NULL, 2, 'https://img.teste/bia.jpg', 'hash-bia',
     now() - INTERVAL '2 days', now() - INTERVAL '2 days'),
    (2, 40, 2, 2, 1, 'https://img.teste/caio.jpg', 'hash-caio',
     now() - INTERVAL '1 day', now() - INTERVAL '1 day');

SELECT setval(
    pg_get_serial_sequence('tb_postagens', 'id_postagem'),
    (SELECT MAX(id_postagem) FROM tb_postagens));
