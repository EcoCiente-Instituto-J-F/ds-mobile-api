DROP TABLE IF EXISTS tb_cursos;

CREATE TABLE tb_cursos (
    id_curso INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    titulo_curso VARCHAR(255) NOT NULL,
    descricao_curso TEXT,
    esta_ativo BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO tb_cursos (
    id_curso,
    titulo_curso,
    descricao_curso,
    esta_ativo
)
OVERRIDING SYSTEM VALUE
VALUES
    (1, 'Reciclagem', 'Como separar resíduos.', TRUE),
    (2, 'Compostagem', NULL, TRUE),
    (3, 'Curso desativado', 'Conteúdo indisponível.', FALSE),
    (4, 'Reciclagem', 'Outro curso com o mesmo título.', TRUE);

ALTER TABLE tb_cursos
    ALTER COLUMN id_curso RESTART WITH 100;