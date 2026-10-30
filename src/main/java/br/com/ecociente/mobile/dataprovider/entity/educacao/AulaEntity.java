package br.com.ecociente.mobile.dataprovider.entity.educacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "tb_aulas",
    uniqueConstraints = @UniqueConstraint(
        name = "aulas_index_4",
        columnNames = {"curso_id", "ordem"}
    )
)
public class AulaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aula")
    private Integer id;

    @Column(name = "curso_id", nullable = false)
    private Integer cursoId;

    @Column(name = "titulo_aula", nullable = false, length = 255)
    private String titulo;

    @Column(name = "conteudo_aula", columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    public AulaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getCursoId() {
        return cursoId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getConteudo() {
        return conteudo;
    }

    public Integer getOrdem() {
        return ordem;
    }
}