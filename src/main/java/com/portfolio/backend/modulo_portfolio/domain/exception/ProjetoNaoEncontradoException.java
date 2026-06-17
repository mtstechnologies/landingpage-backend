package com.portfolio.backend.modulo_portfolio.domain.exception;

import java.util.UUID;

public class ProjetoNaoEncontradoException extends RuntimeException {
    public ProjetoNaoEncontradoException(UUID id) {
        super("Projeto não encontrado com o ID: " + id);
    }
}
