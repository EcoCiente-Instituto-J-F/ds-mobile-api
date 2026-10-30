package br.com.ecociente.mobile.dataprovider.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "tb_recuperacoes_senha")@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsqueceuSenhaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_esqueci_senha")
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private Timestamp expiraEm;

    @Column(name = "utilizado_em")
    private Timestamp utilizadoEm;
  
}
