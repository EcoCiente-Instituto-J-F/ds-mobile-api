package br.com.ecociente.mobile.dataprovider.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_cursos")
public class CursoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curso")
    private Integer id;

    @Column(name = "titulo_curso", nullable = false, length = 255)
    private String titulo;

    @Column(name = "descricao_curso", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "esta_ativo", nullable = false)
    private boolean estaAtivo;

    public CursoEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isEstaAtivo() {
        return estaAtivo;
    }
}