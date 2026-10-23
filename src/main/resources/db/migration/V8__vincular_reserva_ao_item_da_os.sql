CREATE TABLE reserva_item_os (
    item_id UUID PRIMARY KEY,
    ordem_de_servico_id UUID NOT NULL REFERENCES ordem_de_servico (id) ON DELETE CASCADE,
    peca_id UUID NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    reserva_id UUID NOT NULL UNIQUE,
    CONSTRAINT fk_reserva_item_os_item FOREIGN KEY (item_id)
        REFERENCES item_da_os (id) DEFERRABLE INITIALLY DEFERRED
);
CREATE INDEX idx_reserva_item_os_ordem ON reserva_item_os (ordem_de_servico_id);
