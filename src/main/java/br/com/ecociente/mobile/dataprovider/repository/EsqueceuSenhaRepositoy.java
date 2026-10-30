package br.com.ecociente.mobile.dataprovider.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ecociente.mobile.dataprovider.entity.EsqueceuSenhaEntity;
import jakarta.persistence.LockModeType;

public interface EsqueceuSenhaRepositoy
                extends JpaRepository<EsqueceuSenhaEntity, Integer> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        Optional<EsqueceuSenhaEntity> findFirstByUsuarioIdOrderByIdDesc(
                        Integer usuarioId);

        @Modifying(flushAutomatically = true)
        @Query("""
                        UPDATE EsqueceuSenhaEntity r
                        SET r.expiraEm = :agora
                        WHERE r.usuarioId = :usuarioId
                          AND r.utilizadoEm IS NULL
                          AND r.expiraEm > :agora
                        """)
        int invalidarPendentes(
                        @Param("usuarioId") Integer usuarioId,
                        @Param("agora") OffsetDateTime agora);
}