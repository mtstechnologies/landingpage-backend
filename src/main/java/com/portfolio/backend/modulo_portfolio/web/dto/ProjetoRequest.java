package com.portfolio.backend.modulo_portfolio.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProjetoRequest(
        @NotBlank(message = "O título do projeto é obrigatório.")
        String titulo,
        @NotBlank(message = "Slug é obrigatório")
        @Pattern(
            regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
            message = "Slug deve conter apenas letras minúsculas, números e hífens"
        )
        @Size(max = 255)
        String slug,
        String descricao,
        String urlCapa,
        String linkProducao,
        String linkRepositorio,
        LocalDate dataDesenvolvimento,
        Set<UUID> tecnologiaIds
) {}
