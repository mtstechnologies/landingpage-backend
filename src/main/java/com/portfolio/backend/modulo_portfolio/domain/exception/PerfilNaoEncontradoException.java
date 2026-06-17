package com.portfolio.backend.modulo_portfolio.domain.exception;

import java.util.UUID;

public class PerfilNaoEncontradoException extends RuntimeException {
    public PerfilNaoEncontradoException(UUID id) {
        super("Perfil não encontrado com o ID: " + id);
    }
    public PerfilNaoEncontradoException(String message) {
        super(message);
    }
}
