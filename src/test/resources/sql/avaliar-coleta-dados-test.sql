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

INSERT INTO tb_lkp_status_agendamentos (nome_status)
SELECT 'Agendado'
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_lkp_status_agendamentos
    WHERE UPPER(nome_status) = 'AGENDADO'
);

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
    (10, 'Coop Um', 'coop1@teste.com', 'hash', TRUE, 1, 1),
    (30, 'Síndico Um', 'sindico1@teste.com', 'hash', TRUE, 2, 1),
    (31, 'Síndico Dois', 'sindico2@teste.com', 'hash', TRUE, 2, 2);

INSERT INTO tb_sindicos (id_sindico, usuario_id)
OVERRIDING SYSTEM VALUE
VALUES (1, 30), (2, 31);

INSERT INTO tb_condominios (
    id_condominio, nome_condominio,
    ativo, tipo_condominio_id, sindico_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Condomínio Um', TRUE, 1, 1, 1),
    (2, 'Condomínio Dois', TRUE, 1, 2, 2);

INSERT INTO tb_cooperativas (
    id_cooperativa, cnpj_cooperativa,
    nome_cooperativa, usuario_id
)
OVERRIDING SYSTEM VALUE
VALUES (1, '34238864000168', 'Cooperativa Um', 10);

-- 100: realizada, ninguém registrou visita ainda
-- 101: realizada, visita já avaliada
-- 102: apenas agendada
-- 103: realizada, do condomínio do outro síndico
-- 104: realizada, com visita registrada e ainda sem avaliação
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
    v.condominio,
    1,
    (
        SELECT id_status
        FROM tb_lkp_status_agendamentos
        WHERE UPPER(nome_status) = v.status
        ORDER BY id_status
        LIMIT 1
    ),
    TIMESTAMPTZ '2026-01-01 09:00:00-03',
    TIMESTAMPTZ '2026-01-01 10:00:00-03',
    FALSE,
    10,
    'COOPERATIVA'
FROM (
    VALUES
        (100, 1, 'REALIZADO'),
        (101, 1, 'REALIZADO'),
        (102, 1, 'AGENDADO'),
        (103, 2, 'REALIZADO'),
        (104, 1, 'REALIZADO')
) AS v(id, condominio, status);

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
    (1001, 101, '2026-01-01 09:00:00-03', TRUE, TRUE, '2026-01-01 10:00:00-03'),
    (1004, 104, '2026-01-01 09:30:00-03', TRUE, TRUE, '2026-01-01 10:00:00-03');

INSERT INTO tb_avaliacoes_visitas_coletas (
    id_avaliacao,
    visita_coleta_id,
    usuario_avaliador_id,
    nota,
    comentario,
    avaliado_em
)
OVERRIDING SYSTEM VALUE
VALUES (1, 1001, 30, 4, 'Boa coleta', '2026-01-02 12:00:00-03');

-- as próximas linhas geradas pelo banco não podem colidir com os ids acima
SELECT setval(pg_get_serial_sequence('tb_visitas_coletas', 'id_visita_coleta'), 5000);
SELECT setval(pg_get_serial_sequence('tb_avaliacoes_visitas_coletas', 'id_avaliacao'), 5000);
