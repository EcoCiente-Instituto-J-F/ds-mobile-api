package br.com.ecociente.mobile.dataprovider.entity;

import java.time.LocalDateTime;

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
@Table(name = "tb_agendamentos_coletas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendamentoColetaEntity {

  @Id
  @GeneratedValue(strategy= GenerationType.IDENTITY)
  @Column(name = "id_agendamento_coleta", nullable = false)
  private Integer id;

  @Column(name = "condominio_id", nullable = false)
  private Integer condominioId;

  @Column(name = "cooperativa_id", nullable = false)
  private Integer cooperativaId;

  /**
   * status do agendamento, que pode ser: PENDENTE, CONFIRMADO, CANCELADO, CONCLUIDO
  */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "status_agendamento_id", nullable = false)
  private StatusAgendamentoEntity statusAgendamento;

  /**
   * Data de início do agendamento
   */
  @Column(name = "data_inicio", nullable = false)
  private LocalDateTime dataInicio;

  /**
   * Data de fim do agendamento
   */
  @Column(name = "data_fim")
  private LocalDateTime dataFim;

  /**
   * Indica se o agendamento possui recorrência
   */
  @Column(name = "possui_recorrencia", nullable = false)
  private Boolean possuiRecorrencia;

  /**
   * Quem pediu a coleta: CONDOMINIO ou COOPERATIVA
   */
  @Column(name = "origem_solicitacao", length = 20)
  private String origemSolicitacao;

  /**
   * Usuário que pediu a coleta
   */
  @Column(name = "solicitado_por_usuario_id")
  private Integer solicitadoPorUsuarioId;

}
