package br.com.ecociente.mobile.dataprovider.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_rel_recorrencias_agendamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecorrenciaAgendamentoEntity {

  @Id
  @GeneratedValue(strategy= GenerationType.IDENTITY)
  @Column(name = "id_recorrencia_agendamento", nullable = false)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agendamento_coleta_id", nullable = false)
  private AgendamentoColetaEntity agendamentoColeta;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dia_semana_id", nullable = false)
  private DiaSemanaEntity diaSemana;
  
}
