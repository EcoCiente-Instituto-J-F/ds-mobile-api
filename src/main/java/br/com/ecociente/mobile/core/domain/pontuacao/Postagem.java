package br.com.ecociente.mobile.core.domain.pontuacao;

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
public class Postagem {

    private Integer id;
    private Integer usuarioId;
    private Integer condominioId;
    private Integer torreId;
    private Integer categoriaId;
    private Integer statusValidacaoId;
    private Integer saldoConfianca;
    private Boolean pontuacaoAtiva;
    private Boolean pontuacaoReconciliacaoPendente;
    private OffsetDateTime dataPostagem;
    private OffsetDateTime dataLimiteAnalise;
    private OffsetDateTime resolvidoEm;
}