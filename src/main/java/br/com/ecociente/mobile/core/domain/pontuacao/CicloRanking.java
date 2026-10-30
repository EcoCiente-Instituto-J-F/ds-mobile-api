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
public class CicloRanking {

    private String id;
    private OffsetDateTime inicio;
    private OffsetDateTime fim;
}