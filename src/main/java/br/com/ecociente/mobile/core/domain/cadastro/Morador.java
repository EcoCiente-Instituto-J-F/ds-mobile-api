package br.com.ecociente.mobile.core.domain.cadastro;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
public class Morador {
  private Integer idMorador;
  private Integer usuarioId;
  private Integer condominioId;
  private OffsetDateTime dataEntrada;
  private OffsetDateTime dataSaida;
  private Boolean aprovado;
  private Integer aprovadoPorUsuarioId;
  private Integer nivelConfiancaId;
  private BigDecimal trustScore;
  private Integer postagensValidadasSemContestacao;
  private Integer denunciasProcedentes;
  private Integer denunciasRealizadas;
  private BigDecimal taxaAcertoDenuncias;
}
