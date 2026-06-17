package com.portfolio.backend.modulo_portfolio.infrastructure.repository;

import com.portfolio.backend.modulo_portfolio.domain.Perfil;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, UUID> {
    Optional<Perfil> findFirstByOrderByNomeAsc();
}
