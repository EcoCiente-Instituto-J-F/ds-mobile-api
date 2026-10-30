package br.com.ecociente.mobile.dataprovider.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ecociente.mobile.dataprovider.entity.CondominioEntity;

public interface CondominioRepository extends JpaRepository<CondominioEntity,Integer> {
  Optional<CondominioEntity> findByCodigoAcessoIgnoreCase(String codigoAcesso);
  boolean existsByCodigoAcesso(String codigoAcesso);
  boolean existsByCnpj(String cnpj);  
}
