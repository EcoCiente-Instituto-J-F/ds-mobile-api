package br.com.ecociente.mobile.dataprovider.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ecociente.mobile.dataprovider.entity.StatusAgendamentoEntity;

public interface StatusAgendamentoRepository extends JpaRepository<StatusAgendamentoEntity, Integer> {
  StatusAgendamentoEntity findByNomeStatus(String nomeStatus);
}
