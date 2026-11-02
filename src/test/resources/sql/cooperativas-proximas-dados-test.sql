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

-- Permite usar o schema base que ainda não possui estes campos.
-- Não substitui a migração e a trigger reais de coletas.
ALTER TABLE tb_agendamentos_coletas
    ADD COLUMN IF NOT EXISTS solicitado_por_usuario_id INTEGER,
    ADD COLUMN IF NOT EXISTS origem_solicitacao VARCHAR(20);

INSERT INTO tb_lkp_tipos_usuarios (
    id_tipo_usuario, nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Cooperativa'),
    (2, 'Síndico');

INSERT INTO tb_lkp_tipos_condominios (
    id_tipo_condominio, nome_tipo
)
OVERRIDING SYSTEM VALUE
VALUES (1, 'Residencial');

INSERT INTO tb_lkp_status_agendamentos (nome_status)
SELECT 'Realizado'
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_lkp_status_agendamentos
    WHERE UPPER(nome_status) = 'REALIZADO'
);

INSERT INTO tb_enderecos (
    id_endereco, cep, estado, cidade, logradouro, numero, complemento
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '01001000', 'SP', 'São Paulo', 'Rua do Condomínio', '10', NULL),
    (2, '01310100', 'SP', 'São Paulo', 'Avenida Coop A', '100', 'Galpão 2'),
    (3, '13010000', 'SP', 'Campinas', 'Rua Coop B', '200', NULL),
    (4, '01001001', 'SP', 'São Paulo', 'Rua Coop C', '300', NULL),
    (5, '01001002', 'sp', ' são paulo ', 'Rua Coop D', '400', NULL),
    (6, '20010000', 'RJ', 'Rio de Janeiro', 'Rua do Outro Condomínio', '5', NULL),
    (7, '01001003', 'SP', 'São Paulo', 'Rua do Condomínio Inativo', '15', NULL);

INSERT INTO tb_usuarios (
    id_usuario, nome_usuario, email_usuario,
    senha_hash, ativo, tipo_usuario_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (10, 'Coop A', 'coopa@teste.com', 'hash', TRUE, 1, 2),
    (20, 'Coop B', 'coopb@teste.com', 'hash', TRUE, 1, 3),
    (30, 'Síndico de São Paulo', 'sindico@teste.com', 'hash', TRUE, 2, 1),
    (40, 'Coop C Inativa', 'coopc@teste.com', 'hash', FALSE, 1, 4),
    (50, 'Coop D', 'coopd@teste.com', 'hash', TRUE, 1, 5),
    (60, 'Síndico sem condomínio ativo', 'sindico2@teste.com', 'hash', TRUE, 2, 1),
    (70, 'Síndico do Rio', 'sindico3@teste.com', 'hash', TRUE, 2, 6);

INSERT INTO tb_sindicos (id_sindico, usuario_id)
OVERRIDING SYSTEM VALUE
VALUES (1, 30), (2, 60), (3, 70);

INSERT INTO tb_condominios (
    id_condominio, nome_condominio,
    ativo, tipo_condominio_id, sindico_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Condomínio São Paulo', TRUE, 1, 1, 1),
    (2, 'Condomínio Inativo', FALSE, 1, 2, 7),
    (3, 'Condomínio Rio', TRUE, 1, 3, 6);

INSERT INTO tb_cooperativas (
    id_cooperativa, cnpj_cooperativa,
    nome_cooperativa, telefone_cooperativa, usuario_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '34238864000168', 'Cooperativa A', '11999990000', 10, 2),
    (2, '11222333000181', 'Cooperativa B (Campinas)', NULL, 20, 3),
    (3, '22333444000181', 'Cooperativa C (inativa)', NULL, 40, 4),
    (4, '33444555000181', 'Cooperativa D', NULL, 50, 5),
    (5, '44555666000181', 'Cooperativa E sem endereço', NULL, NULL, NULL);

INSERT INTO tb_agendamentos_coletas (
    id_agendamento_coleta,
    condominio_id,
    cooperativa_id,
    status_agendamento_id,
    data_inicio,
    data_fim,
    possui_recorrencia,
    solicitado_por_usuario_id,
    origem_solicitacao
)
OVERRIDING SYSTEM VALUE
SELECT
    v.id,
    1,
    1,
    (
        SELECT id_status
        FROM tb_lkp_status_agendamentos
        WHERE UPPER(nome_status) = 'REALIZADO'
        ORDER BY id_status
        LIMIT 1
    ),
    TIMESTAMPTZ '2026-01-01 09:00:00-03',
    TIMESTAMPTZ '2026-01-01 10:00:00-03',
    FALSE,
    10,
    'COOPERATIVA'
FROM (VALUES (100), (101)) AS v(id);

INSERT INTO tb_visitas_coletas (
    id_visita_coleta,
    agendamento_coleta_id,
    data_visita,
    foi_realizada,
    houve_confirmacao,
    confirmado_em
)
OVERRIDING SYSTEM VALUE
VALUES
    (1000, 100, '2026-01-01 09:00:00-03', TRUE, TRUE, '2026-01-01 10:00:00-03'),
    (1001, 101, '2026-01-01 09:00:00-03', TRUE, TRUE, '2026-01-01 10:00:00-03');

INSERT INTO tb_avaliacoes_visitas_coletas (
    id_avaliacao,
    visita_coleta_id,
    usuario_avaliador_id,
    nota,
    comentario,
    avaliado_em
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 1000, 30, 5, 'Excelente', '2026-01-04 12:00:00-03'),
    (2, 1001, 30, 3, NULL, '2026-01-03 12:00:00-03');
