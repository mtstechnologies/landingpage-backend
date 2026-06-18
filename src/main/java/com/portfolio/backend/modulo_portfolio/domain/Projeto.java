package com.portfolio.backend.modulo_portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projeto")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "url_capa")
    private String urlCapa;

    @Column(name = "link_producao")
    private String linkProducao;

    @Column(name = "link_repositorio")
    private String linkRepositorio;

    @Column(name = "data_desenvolvimento")
    private LocalDate dataDesenvolvimento;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "projeto_tecnologia",
        joinColumns = @JoinColumn(name = "projeto_id"),
        inverseJoinColumns = @JoinColumn(name = "tecnologia_id")
    )
    @Builder.Default
    private Set<Tecnologia> tecnologias = new HashSet<>();

    @Version
    @Column(nullable = false)
    private Integer versao;
}
