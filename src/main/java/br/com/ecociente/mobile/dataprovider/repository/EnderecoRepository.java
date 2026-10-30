package br.com.ecociente.mobile.dataprovider.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.ecociente.mobile.dataprovider.entity.EnderecoEntity;

public interface EnderecoRepository
    extends JpaRepository<EnderecoEntity, Integer> {
}