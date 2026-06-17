package com.portfolio.backend.modulo_portfolio.web.dto;

import jakarta.validation.constraints.NotBlank;

public record TecnologiaRequest(
        @NotBlank(message = "O nome da tecnologia é obrigatório.")
        String nome,
        String urlIcone
) {}
