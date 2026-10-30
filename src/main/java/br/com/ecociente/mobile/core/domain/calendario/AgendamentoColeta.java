package br.com.ecociente.mobile.core.domain.calendario;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentoColeta {
  private Integer id;
  private Integer condominioId;
  private Integer cooperativaId;
  private StatusType statusAgendamento;
  private LocalDateTime dataInicio;
  private LocalDateTime dataFim;
  private Boolean possuiRecorrencia;
  private String origemSolicitacao;
  private Integer solicitadoPorUsuarioId;
  private String cooperativaNome;
  private String condominioNome;

}
