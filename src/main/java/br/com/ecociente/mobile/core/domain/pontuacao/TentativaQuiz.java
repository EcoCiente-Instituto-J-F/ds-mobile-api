package br.com.ecociente.mobile.core.domain.pontuacao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TentativaQuiz {

    private Integer id;
    private Integer usuarioId;
    private Integer quizId;
    private Integer condominioId;
    private Integer torreId;
    private BigDecimal nota;
    private Boolean aprovado;
    private OffsetDateTime iniciadoEm;
    private OffsetDateTime concluidoEm;
}