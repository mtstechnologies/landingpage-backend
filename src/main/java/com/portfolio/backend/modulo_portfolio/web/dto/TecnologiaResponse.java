package com.portfolio.backend.modulo_portfolio.web.dto;

import com.portfolio.backend.modulo_portfolio.domain.Tecnologia;
import java.util.UUID;

public record TecnologiaResponse(
        UUID id,
        String nome,
        String urlIcone,
        Integer versao
) {
    public static TecnologiaResponse fromEntity(Tecnologia tecnologia) {
        if (tecnologia == null) return null;
        return new TecnologiaResponse(
                tecnologia.getId(),
                tecnologia.getNome(),
                tecnologia.getUrlIcone(),
                tecnologia.getVersao()
        );
    }
}
