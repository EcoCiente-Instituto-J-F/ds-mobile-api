package br.com.ecociente.mobile.dataprovider.repository;

import br.com.ecociente.mobile.dataprovider.entity.EsqueceuSenhaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.Optional;

public interface EsqueceuSenhaRepositoy
        extends JpaRepository<EsqueceuSenhaEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EsqueceuSenhaEntity> findFirstByUsuarioIdOrderByIdDesc(
            Integer usuarioId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update EsqueceuSenhaEntity r
            set r.expiraEm = :agora
            where r.usuarioId = :usuarioId
              and r.utilizadoEm is null
              and r.expiraEm > :agora
            """)
    int invalidarPendentes(
            @Param("usuarioId") Integer usuarioId,
            @Param("agora") Timestamp agora
    );
}