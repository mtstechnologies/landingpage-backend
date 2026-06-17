package com.portfolio.backend.modulo_portfolio.infrastructure.repository;

import com.portfolio.backend.modulo_portfolio.domain.Projeto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {
    @Query("SELECT DISTINCT p FROM Projeto p LEFT JOIN FETCH p.tecnologias ORDER BY p.dataDesenvolvimento DESC")
    List<Projeto> findAllWithTecnologiasOrderByDataDesenvolvimentoDesc();
}
