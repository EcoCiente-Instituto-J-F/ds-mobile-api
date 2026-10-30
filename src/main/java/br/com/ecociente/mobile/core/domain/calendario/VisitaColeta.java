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
public class VisitaColeta {
  
  private Integer id;
  private LocalDateTime dataVisita;
  private Boolean realizada;
  private Boolean houveConfirmacao;
  private LocalDateTime confirmadoEm;
  private String observacao;
  private AgendamentoColeta agendamentoColeta;
}
