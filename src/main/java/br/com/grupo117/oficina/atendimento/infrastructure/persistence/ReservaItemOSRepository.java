package br.com.grupo117.oficina.atendimento.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Vínculo de integração de Atendimento; não modifica as entidades do contexto Estoque. */
@Repository
public class ReservaItemOSRepository {
    private final JdbcTemplate jdbc;
    public ReservaItemOSRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Vinculo(UUID itemId, UUID osId, UUID pecaId, int quantidade, UUID reservaId) {}
    public record Reserva(UUID id, UUID pecaId, int quantidade, String status) {}

    public Optional<Vinculo> buscarPorItem(UUID itemId) {
        return jdbc.query("select * from reserva_item_os where item_id = ?", (rs, n) ->
                new Vinculo(rs.getObject("item_id", UUID.class), rs.getObject("ordem_de_servico_id", UUID.class),
                        rs.getObject("peca_id", UUID.class), rs.getInt("quantidade"), rs.getObject("reserva_id", UUID.class)), itemId)
                .stream().findFirst();
    }

    public List<Vinculo> buscarPorOrdem(UUID osId) {
        return jdbc.query("select * from reserva_item_os where ordem_de_servico_id = ? order by item_id", (rs, n) ->
                new Vinculo(rs.getObject("item_id", UUID.class), rs.getObject("ordem_de_servico_id", UUID.class),
                        rs.getObject("peca_id", UUID.class), rs.getInt("quantidade"), rs.getObject("reserva_id", UUID.class)), osId);
    }

    public void salvar(Vinculo v) {
        jdbc.update("insert into reserva_item_os (item_id, ordem_de_servico_id, peca_id, quantidade, reserva_id) values (?, ?, ?, ?, ?)",
                v.itemId(), v.osId(), v.pecaId(), v.quantidade(), v.reservaId());
    }

    public List<Reserva> buscarReservas(UUID osId) {
        return jdbc.query("select id, peca_id, quantidade, status from reserva_de_estoque where ordem_de_servico_id = ? order by id",
                (rs, n) -> new Reserva(rs.getObject("id", UUID.class), rs.getObject("peca_id", UUID.class),
                        rs.getInt("quantidade"), rs.getString("status")), osId);
    }
}
