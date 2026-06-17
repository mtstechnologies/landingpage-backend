package com.portfolio.backend.modulo_portfolio.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProjetoRequest(
        @NotBlank(message = "O título do projeto é obrigatório.")
        String titulo,
        String descricao,
        String urlCapa,
        String linkProducao,
        String linkRepositorio,
        LocalDate dataDesenvolvimento,
        Set<UUID> tecnologiaIds
) {}
