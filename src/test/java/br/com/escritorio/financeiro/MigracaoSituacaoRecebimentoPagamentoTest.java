package br.com.escritorio.financeiro;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Roda as migrations num esquema separado até a V19, grava pagamentos nas três situações antigas e confere que a
 * V21 leva "Pendente" para "Agendado" e "Pago" para "Recebido", sem aceitar mais as situações antigas.
 */
class MigracaoSituacaoRecebimentoPagamentoTest extends IntegracaoTest {

    private static final String ESQUEMA = "teste_migracao_v21";

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
    void pendenteViraAgendadoEPagoViraRecebido() {
        migrarAte("19");
        Long tipo = inserir("insert into %s.tipo_servico (nome, criado_em, atualizado_em) "
                + "values ('Benefício Migração', now(), now()) returning id");
        Long cliente = inserir("insert into %s.cliente (nome, data_cadastro, criado_em, atualizado_em) "
                + "values ('Maria Migração', current_date, now(), now()) returning id");
        Long agendado = inserirPagamento(cliente, tipo, "AGENDADO");
        Long pago = inserirPagamento(cliente, tipo, "PAGO");
        Long pendente = inserirPagamento(cliente, tipo, "PENDENTE");

        migrarAte("21");

        assertThat(status(agendado)).isEqualTo("AGENDADO");
        assertThat(status(pago)).isEqualTo("RECEBIDO");
        assertThat(status(pendente)).isEqualTo("AGENDADO");
        assertThatThrownBy(() -> inserirPagamento(cliente, tipo, "PENDENTE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private String status(Long pagamento) {
        return jdbc.queryForObject("select status from " + ESQUEMA + ".pagamento_agendado where id = ?",
                String.class, pagamento);
    }

    private Long inserirPagamento(Long cliente, Long tipo, String status) {
        return jdbc.queryForObject(("insert into %s.pagamento_agendado (cliente_id, tipo_servico_id, "
                        + "data_recebimento, valor, banco, status, criado_em, atualizado_em) "
                        + "values (?, ?, current_date, 1500.00, 'Banco do Brasil', ?, now(), now()) "
                        + "returning id").formatted(ESQUEMA),
                Long.class, cliente, tipo, status);
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
