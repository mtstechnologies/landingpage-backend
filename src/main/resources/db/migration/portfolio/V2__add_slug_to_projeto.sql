ALTER TABLE projeto ADD COLUMN slug VARCHAR(255);

UPDATE projeto
SET slug = LOWER(
  REGEXP_REPLACE(
    REGEXP_REPLACE(titulo, '[^a-zA-Z0-9\s-]', '', 'g'),
    '\s+', '-', 'g'
  )
)
WHERE slug IS NULL;

ALTER TABLE projeto ALTER COLUMN slug SET NOT NULL;
ALTER TABLE projeto ADD CONSTRAINT projeto_slug_unique UNIQUE (slug);
CREATE INDEX idx_projeto_slug ON projeto(slug);
