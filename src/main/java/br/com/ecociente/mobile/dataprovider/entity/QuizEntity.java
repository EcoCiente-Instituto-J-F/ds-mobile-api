package br.com.ecociente.mobile.dataprovider.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_quizzes")
public class QuizEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_quiz")
    private Integer id;

    @Column(name = "curso_id", nullable = false)
    private Integer cursoId;

    @Column(name = "aula_id", nullable = false, unique = true)
    private Integer aulaId;

    @Column(name = "titulo_quiz", nullable = false, length = 255)
    private String titulo;

    @Column(
        name = "nota_minima_aprovacao",
        nullable = false,
        precision = 5,
        scale = 2
    )
    private BigDecimal notaMinimaAprovacao;

    @Column(name = "pontos_recompensa", nullable = false)
    private Integer pontosRecompensa;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    public QuizEntity() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getCursoId() {
        return cursoId;
    }

    public Integer getAulaId() {
        return aulaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public BigDecimal getNotaMinimaAprovacao() {
        return notaMinimaAprovacao;
    }

    public Integer getPontosRecompensa() {
        return pontosRecompensa;
    }
}