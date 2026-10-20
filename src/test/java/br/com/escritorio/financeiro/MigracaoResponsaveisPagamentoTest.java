package br.com.escritorio.financeiro;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roda as migrations num esquema separado até a V18, grava um pagamento no formato antigo (um responsável e o
 * repasse no próprio pagamento) e confere que a V19 leva o responsável e o repasse para a nova tabela.
 */
class MigracaoResponsaveisPagamentoTest extends IntegracaoTest {

    private static final String ESQUEMA = "teste_migracao_v19";

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        apagarEsquema();
    }

    @AfterEach
    void apagarEsquema() {
        jdbc.execute("drop schema if exists " + ESQUEMA + " cascade");
    }

    @Test
    void pagamentoAntigoViraUmaLinhaDeResponsavelComOMesmoRepasse() {
        migrarAte("18");
        Long responsavel = inserir("insert into %s.responsavel (nome, criado_em, atualizado_em) "
                + "values ('Ana Migração', now(), now()) returning id");
        Long tipo = inserir("insert into %s.tipo_servico (nome, criado_em, atualizado_em) "
                + "values ('Benefício Migração', now(), now()) returning id");
        Long cliente = inserir("insert into %s.cliente (nome, data_cadastro, criado_em, atualizado_em) "
                + "values ('Maria Migração', current_date, now(), now()) returning id");
        Long comRepasse = inserirPagamento(cliente, responsavel, tipo, "150.00", "PAGO");
        Long semRepasse = inserirPagamento(cliente, responsavel, tipo, null, "PENDENTE");

        migrarAte("19");

        Map<String, Object> linha = jdbc.queryForMap("select responsavel_id, valor_repasse, situacao_repasse from "
                + ESQUEMA + ".pagamento_responsavel where pagamento_agendado_id = ?", comRepasse);
        assertThat(linha.get("responsavel_id")).isEqualTo(responsavel);
        assertThat((BigDecimal) linha.get("valor_repasse")).isEqualByComparingTo("150.00");
        assertThat(linha.get("situacao_repasse")).isEqualTo("PAGO");

        Map<String, Object> vazia = jdbc.queryForMap("select valor_repasse, situacao_repasse from "
                + ESQUEMA + ".pagamento_responsavel where pagamento_agendado_id = ?", semRepasse);
        assertThat(vazia.get("valor_repasse")).isNull();
        assertThat(vazia.get("situacao_repasse")).isEqualTo("PENDENTE");

        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_schema = ? "
                        + "and table_name = 'pagamento_agendado' and column_name in "
                        + "('responsavel_id', 'valor_repasse', 'situacao_repasse')",
                Long.class, ESQUEMA)).isZero();
    }

    private Long inserirPagamento(Long cliente, Long responsavel, Long tipo, String repasse, String situacao) {
        return jdbc.queryForObject(("insert into %s.pagamento_agendado (cliente_id, responsavel_id, tipo_servico_id, "
                        + "data_recebimento, valor, banco, status, valor_repasse, situacao_repasse, criado_em, atualizado_em) "
                        + "values (?, ?, ?, current_date, 1500.00, 'Banco do Brasil', 'AGENDADO', ?, ?, now(), now()) "
                        + "returning id").formatted(ESQUEMA),
                Long.class, cliente, responsavel, tipo, repasse == null ? null : new BigDecimal(repasse), situacao);
    }

    private Long inserir(String sql) {
        return jdbc.queryForObject(sql.formatted(ESQUEMA), Long.class);
    }

    private void migrarAte(String versao) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(ESQUEMA)
                .defaultSchema(ESQUEMA)
                .locations("classpath:db/migration")
                .target(versao)
                .load()
                .migrate();
    }
}
