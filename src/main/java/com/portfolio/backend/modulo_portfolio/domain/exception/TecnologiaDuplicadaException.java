package com.portfolio.backend.modulo_portfolio.domain.exception;

public class TecnologiaDuplicadaException extends RuntimeException {
    public TecnologiaDuplicadaException(String nome) {
        super("Tecnologia com o nome '" + nome + "' já cadastrada.");
    }
}
