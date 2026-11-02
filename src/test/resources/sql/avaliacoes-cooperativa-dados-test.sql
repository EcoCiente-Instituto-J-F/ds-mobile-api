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
    (20, 'Coop Dois', 'coop2@teste.com', 'hash', TRUE, 1, 2),
    (30, 'Síndico', 'sindico@teste.com', 'hash', TRUE, 2, 1),
    (40, 'Coop Sem Avaliação', 'coop3@teste.com', 'hash', TRUE, 1, 1),
    (50, 'Coop Inativa', 'coop4@teste.com', 'hash', FALSE, 1, 2);

INSERT INTO tb_sindicos (id_sindico, usuario_id)
OVERRIDING SYSTEM VALUE
VALUES (1, 30);

INSERT INTO tb_condominios (
    id_condominio, nome_condominio,
    ativo, tipo_condominio_id, sindico_id, endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Condomínio Um', TRUE, 1, 1, 1),
    (2, 'Condomínio Dois', TRUE, 1, 1, 2);

INSERT INTO tb_cooperativas (
    id_cooperativa, cnpj_cooperativa,
    nome_cooperativa, usuario_id
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, '34238864000168', 'Cooperativa Um', 10),
    (2, '11222333000181', 'Cooperativa Dois', 20),
    (3, '22333444000181', 'Cooperativa Sem Avaliação', 40),
    (4, '33444555000181', 'Cooperativa Inativa', 50);

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
    v.cooperativa,
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
    v.solicitante,
    'COOPERATIVA'
FROM (
    VALUES
        (100, 1, 1, 10),
        (101, 2, 1, 10),
        (102, 1, 1, 10),
        (200, 1, 2, 20)
) AS v(id, condominio, cooperativa, solicitante);

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
    (1001, 101, '2026-01-01 09:00:00-03', TRUE, TRUE, '2026-01-01 10:00:00-03'),
    (1002, 102, '2026-01-01 09:00:00-03', TRUE, TRUE, '2026-01-01 10:00:00-03'),
    (2000, 200, '2026-01-01 09:00:00-03', TRUE, TRUE, '2026-01-01 10:00:00-03');

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
    (1, 1000, 30, 5, 'Excelente atendimento', '2026-01-04 12:00:00-03'),
    (2, 1001, 30, 3, NULL, '2026-01-03 12:00:00-03'),
    (3, 1002, 30, 4, 'Boa coleta', '2026-01-02 12:00:00-03'),
    (4, 2000, 30, 1, 'Avaliação da outra cooperativa', '2026-01-05 12:00:00-03');