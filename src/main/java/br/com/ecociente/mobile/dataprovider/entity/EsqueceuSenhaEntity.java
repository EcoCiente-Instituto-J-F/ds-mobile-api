package br.com.ecociente.mobile.dataprovider.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_recuperacoes_senha")
@Getter
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

    @TimeZoneStorage(TimeZoneStorageType.NATIVE)
    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    @TimeZoneStorage(TimeZoneStorageType.NATIVE)
    @Column(name = "utilizado_em")
    private OffsetDateTime utilizadoEm;
}