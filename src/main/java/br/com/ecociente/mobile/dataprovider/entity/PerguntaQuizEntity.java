package br.com.ecociente.mobile.dataprovider.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "tb_perguntas_quiz",
    uniqueConstraints = @UniqueConstraint(
        name = "perguntas_quiz_index_12",
        columnNames = {"quiz_id", "ordem"}
    )
)
public class PerguntaQuizEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pergunta")
    private Integer id;

    @Column(name = "quiz_id", nullable = false)
    private Integer quizId;

    @Column(name = "enunciado", nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    public PerguntaQuizEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public Integer getOrdem() {
        return ordem;
    }
}