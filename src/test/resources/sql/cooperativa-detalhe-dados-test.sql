-- Este complemento é executado depois de:
-- avaliacoes-cooperativa-dados-test.sql

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
    (
        3, '13010000', 'SP', 'Campinas',
        'Rua Campinas', '30'
    ),
    (
        4, '20010000', 'RJ', 'São Paulo',
        'Rua Outro Estado', '40'
    ),
    (
       5, '01001000', 'sp', ' são paulo ',
        'Rua da Cooperativa', '100'
    );

-- Mesmo município e estado, com diferenças de caixa e espaços.
UPDATE tb_cooperativas
SET endereco_id = 5,
    email_cooperativa = 'contato@teste.com',
    telefone_cooperativa = '11999990000'
WHERE id_cooperativa = 1;

UPDATE tb_cooperativas
SET endereco_id = 2
WHERE id_cooperativa = 2;

-- Responsável ativo, mas em outra cidade.
UPDATE tb_cooperativas
SET endereco_id = 3
WHERE id_cooperativa = 3;

-- Mesmo município, mas responsável inativo.
UPDATE tb_cooperativas
SET endereco_id = 1
WHERE id_cooperativa = 4;

-- Mesmo nome de cidade, mas outro estado.
-- Sem responsável: segue a regra existente da listagem.
INSERT INTO tb_cooperativas (
    id_cooperativa,
    cnpj_cooperativa,
    nome_cooperativa,
    endereco_id
)
OVERRIDING SYSTEM VALUE
VALUES (
    5,
    '44555666000181',
    'Cooperativa Outro Estado',
    4
);