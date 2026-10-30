package br.com.ecociente.mobile.core.domain.calendario;

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
public class RecorrenciaAgendamento {
  private Integer id;
  private AgendamentoColeta agendamentoColeta;
  private DiaSemanaType diaSemana;
}
