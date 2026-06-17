package com.portfolio.backend.modulo_portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "perfil")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "link_linkedin")
    private String linkLinkedin;

    @Column(name = "link_github")
    private String linkGithub;

    @Column(name = "url_foto")
    private String urlFoto;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Version
    @Column(nullable = false)
    private Integer versao;
}
