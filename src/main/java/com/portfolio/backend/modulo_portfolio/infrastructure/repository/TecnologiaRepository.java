package com.portfolio.backend.modulo_portfolio.infrastructure.repository;

import com.portfolio.backend.modulo_portfolio.domain.Tecnologia;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TecnologiaRepository extends JpaRepository<Tecnologia, UUID> {
    Optional<Tecnologia> findByNome(String nome);
    boolean existsByNome(String nome);
}
