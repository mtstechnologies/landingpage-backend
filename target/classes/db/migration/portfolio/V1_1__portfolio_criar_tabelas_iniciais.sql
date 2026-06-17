CREATE TABLE perfil (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    bio TEXT,
    link_linkedin VARCHAR(255),
    link_github VARCHAR(255),
    url_foto VARCHAR(255),
    usuario_id UUID,
    versao INT NOT NULL DEFAULT 0
);

CREATE TABLE projeto (
    id UUID PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    url_capa VARCHAR(255),
    link_producao VARCHAR(255),
    link_repositorio VARCHAR(255),
    data_desenvolvimento DATE,
    versao INT NOT NULL DEFAULT 0
);

CREATE TABLE tecnologia (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    url_icone VARCHAR(255),
    versao INT NOT NULL DEFAULT 0
);

CREATE TABLE projeto_tecnologia (
    projeto_id UUID NOT NULL,
    tecnologia_id UUID NOT NULL,
    PRIMARY KEY (projeto_id, tecnologia_id),
    CONSTRAINT fk_projeto_tecnologia_projeto FOREIGN KEY (projeto_id) REFERENCES projeto(id) ON DELETE CASCADE,
    CONSTRAINT fk_projeto_tecnologia_tecnologia FOREIGN KEY (tecnologia_id) REFERENCES tecnologia(id) ON DELETE CASCADE
);

CREATE INDEX idx_perfil_usuario_id ON perfil(usuario_id);
