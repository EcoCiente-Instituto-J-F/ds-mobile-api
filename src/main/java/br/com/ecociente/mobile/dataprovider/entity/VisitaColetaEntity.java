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
@Table(name = "tb_visitas_coletas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitaColetaEntity {
  
  @Id
  @GeneratedValue(strategy= GenerationType.IDENTITY)
  @Column(name = "id_visita_coleta", nullable = false)
  private Integer id;

  /**
   * Agendamento de coleta associado à visita, que pode ser: PENDENTE, CONFIRMADO, CANCELADO, CONCLUIDO
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agendamento_coleta_id", nullable = false)
  private AgendamentoColetaEntity agendamentoColeta;

  @Column(name = "data_visita", nullable = false)
  private LocalDateTime dataVisita;

  @Column(name = "foi_realizada", nullable = false)
  private Boolean realizada;

  @Column(name = "houve_confirmacao", nullable = false)
  private Boolean houveConfirmacao;

  @Column(name = "confirmado_em")
  private LocalDateTime confirmadoEm;

  @Column(name = "observacao")
  private String observacao;
}
