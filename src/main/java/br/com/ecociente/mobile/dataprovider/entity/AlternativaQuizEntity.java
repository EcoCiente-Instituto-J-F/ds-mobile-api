package br.com.ecociente.mobile.dataprovider.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_alternativas_quiz")
public class AlternativaQuizEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alternativa")
    private Integer id;

    @Column(name = "pergunta_id", nullable = false)
    private Integer perguntaId;

    @Column(name = "texto_alternativa", nullable = false, length = 500)
    private String texto;

    @Column(name = "correta", nullable = false)
    private boolean correta;

    public AlternativaQuizEntity() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getPerguntaId() {
        return perguntaId;
    }

    public String getTexto() {
        return texto;
    }
}