-- PREPARACAO_COMPARTILHADA_TESTES
SET search_path TO public;

INSERT INTO public.tb_lkp_tipos_eventos_auditados (nome_evento)
SELECT evento
FROM (
    VALUES
        ('usuario_condominio'),
        ('agendamento_coleta'),
        ('postagem')
) AS eventos(evento)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.tb_lkp_tipos_eventos_auditados existente
    WHERE existente.nome_evento = eventos.evento
);

INSERT INTO public.tb_lkp_tipos_operacoes_auditoria (nome_operacao)
SELECT operacao
FROM (
    VALUES ('INSERT'), ('UPDATE'), ('DELETE')
) AS operacoes(operacao)
WHERE NOT EXISTS (
    SELECT 1
    FROM public.tb_lkp_tipos_operacoes_auditoria existente
    WHERE existente.nome_operacao = operacoes.operacao
);

INSERT INTO tb_lkp_tipos_usuarios (
    id_tipo_usuario,
    nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Síndico'),
    (2, 'Morador');

INSERT INTO tb_lkp_tipos_condominios (
    id_tipo_condominio,
    nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Residencial');

-- O status em análise usa ID 9 para garantir que a consulta
-- utiliza o nome do status, sem depender de um ID fixo.
INSERT INTO tb_lkp_status_validacoes_postagens (
    id_status_validacao,
    nome_status
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'aprovada'),
    (2, 'reprovada'),
    (9, 'em_analise');

INSERT INTO tb_lkp_categorias_residuos (
    id_categoria,
    nome_categoria,
    pontos_base
)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Plástico', 10);

INSERT INTO tb_enderecos (
    id_endereco,
    cep,
    estado,
    cidade,
    logradouro,
    numero
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '01001000', 'SP', 'São Paulo', 'Rua Um', '10'),
    (2, '01001000', 'SP', 'São Paulo', 'Rua Dois', '20'),
    (3, '01001000', 'SP', 'São Paulo', 'Rua Três', '30'),
    (4, '01001000', 'SP', 'São Paulo', 'Rua Quatro', '40'),
    (5, '01001000', 'SP', 'São Paulo', 'Rua Cinco', '50'),
    (6, '01001000', 'SP', 'São Paulo', 'Rua Seis', '60');

INSERT INTO tb_usuarios (
    id_usuario,
    nome_usuario,
    email_usuario,
    senha_hash,
    ativo,
    tipo_usuario_id,
    endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (30, 'Síndico Um', 'sindico1@teste.com', 'hash', TRUE, 1, 1),
    (40, 'Síndico Dois', 'sindico2@teste.com', 'hash', TRUE, 1, 2),
    (50, 'Morador Teste', 'morador@teste.com', 'hash', TRUE, 2, 1),
    (60, 'Síndico Inativo', 'inativo@teste.com', 'hash', FALSE, 1, 5);

INSERT INTO tb_sindicos (
    id_sindico,
    usuario_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 30),
    (2, 40),
    (3, 60);

INSERT INTO tb_condominios (
    id_condominio,
    nome_condominio,
    ativo,
    tipo_condominio_id,
    sindico_id,
    endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Condomínio Um', TRUE, 1, 1, 1),
    (2, 'Condomínio Dois', TRUE, 1, 2, 2),
    (3, 'Sem Postagens', TRUE, 1, 1, 3),
    (4, 'Condomínio Inativo', FALSE, 1, 1, 4),
    (5, 'Síndico Inativo', TRUE, 1, 3, 5),
    (6, 'Sem Síndico', TRUE, 1, NULL, 6);

INSERT INTO tb_torres (
    id_torre,
    nome_torre,
    condominio_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Torre Um', 1),
    (2, 'Torre Dois', 2);

INSERT INTO tb_postagens (
    id_postagem,
    usuario_id,
    condominio_id,
    torre_id,
    categoria_id,
    url_foto,
    hash_foto,
    capturada_em,
    data_postagem,
    status_validacao_id,
    saldo_confianca,
    data_limite_analise,
    resolvido_em
)
OVERRIDING SYSTEM VALUE
SELECT
    v.id,
    50,
    v.condominio,
    v.torre,
    1,
    'https://img.teste/postagem-' || v.id || '.jpg',
    'hash-postagem-' || v.id,
    TIMESTAMPTZ '2026-01-01 10:00:00+00'
        + v.dia * INTERVAL '1 day',
    TIMESTAMPTZ '2026-01-01 10:00:00+00'
        + v.dia * INTERVAL '1 day',
    v.status,
    -2,
    TIMESTAMPTZ '2026-01-02 10:00:00+00'
        + v.dia * INTERVAL '1 day',
    CASE
        WHEN v.resolvida
        THEN TIMESTAMPTZ '2026-01-02 11:00:00+00'
            + v.dia * INTERVAL '1 day'
        ELSE NULL
    END
FROM (
    VALUES
        (100, 1, 1,    9, 0, TRUE),
        (101, 1, NULL, 9, 1, FALSE),
        (102, 1, 1,    9, 1, TRUE),
        (103, 1, 1,    1, 2, TRUE),
        (104, 1, 1,    2, 3, TRUE),
        (200, 2, 2,    9, 0, TRUE)
) AS v(id, condominio, torre, status, dia, resolvida);