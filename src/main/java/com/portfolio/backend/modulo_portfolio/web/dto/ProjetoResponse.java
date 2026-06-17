package com.portfolio.backend.modulo_portfolio.web.dto;

import com.portfolio.backend.modulo_portfolio.domain.Projeto;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ProjetoResponse(
        UUID id,
        String titulo,
        String descricao,
        String urlCapa,
        String linkProducao,
        String linkRepositorio,
        LocalDate dataDesenvolvimento,
        Set<TecnologiaResponse> tecnologias,
        Integer versao
) {
    public static ProjetoResponse fromEntity(Projeto projeto) {
        Set<TecnologiaResponse> techs = projeto.getTecnologias() != null ?
                projeto.getTecnologias().stream()
                        .map(TecnologiaResponse::fromEntity)
                        .collect(Collectors.toSet()) : Set.of();

        return new ProjetoResponse(
                projeto.getId(),
                projeto.getTitulo(),
                projeto.getDescricao(),
                projeto.getUrlCapa(),
                projeto.getLinkProducao(),
                projeto.getLinkRepositorio(),
                projeto.getDataDesenvolvimento(),
                techs,
                projeto.getVersao()
        );
    }
}
