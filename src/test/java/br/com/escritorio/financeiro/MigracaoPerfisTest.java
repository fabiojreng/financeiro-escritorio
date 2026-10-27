package br.com.escritorio.financeiro;

import br.com.escritorio.financeiro.domain.ModuloPermissao;
import br.com.escritorio.financeiro.domain.Permissao;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roda as migrations num esquema separado até a V19, grava usuários com o perfil fixo antigo (ADMIN/OPERADOR) e
 * confere que a V20 cria os perfis Administrador e Operador no banco e vincula cada usuário ao perfil certo.
 */
class MigracaoPerfisTest extends IntegracaoTest {

    private static final String ESQUEMA = "teste_migracao_v20";

    /** Permissões criadas junto com o módulo de perícias (V26). */
    private static final Set<Permissao> PERMISSOES_V26 = EnumSet.of(Permissao.PERICIAS_VISUALIZAR,
            Permissao.PERICIAS_EDITAR, Permissao.PERICIAS_ALTERAR_SITUACAO, Permissao.CADASTROS_TIPOS_PERICIA,
            Permissao.CADASTROS_LOCAIS_PERICIA);

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
    void usuariosAntigosSaoVinculadosAosPerfisDoBanco() {
        migrarAte("19");
        Long admin = inserirUsuario("admin", "ADMIN");
        Long operador = inserirUsuario("operador", "OPERADOR");

        migrarAte("20");

        assertThat(nomeDoPerfil(admin)).isEqualTo("Administrador");
        assertThat(nomeDoPerfil(operador)).isEqualTo("Operador");
        assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_schema = ? "
                + "and table_name = 'usuario' and column_name = 'perfil'", Long.class, ESQUEMA)).isZero();
    }

    @Test
    void administradorRecebeTodasAsPermissoesEOperadorSoAsDaRotina() {
        migrarAte("20");

        assertThat(permissoes("Administrador")).containsExactlyInAnyOrderElementsOf(Arrays.stream(Permissao.values())
                .filter(p -> p.getModulo() != ModuloPermissao.FUNCIONARIOS && !PERMISSOES_V26.contains(p))
                .map(Enum::name)
                .toList());
        assertThat(jdbc.queryForObject("select sistema from " + ESQUEMA + ".perfil where nome = 'Administrador'",
                Boolean.class)).isTrue();

        assertThat(permissoes("Operador"))
                .contains("CLIENTES_VISUALIZAR", "AGENDA_EDITAR", "HONORARIOS_EDITAR", "DESPESAS_ENVIAR_FECHAMENTO")
                .doesNotContain("REPASSES_VISUALIZAR", "REPASSES_EDITAR", "RELATORIOS_VISUALIZAR",
                        "DESPESAS_REABRIR_FECHAMENTO", "ADMIN_USUARIOS", "ADMIN_PERFIS", "ADMIN_AUDITORIA");
    }

    @Test
    void moduloDeFuncionariosSoEntraNoPerfilAdministrador() {
        migrarAte("23");

        assertThat(permissoes("Administrador")).containsExactlyInAnyOrderElementsOf(Arrays.stream(Permissao.values())
                .filter(p -> !PERMISSOES_V26.contains(p))
                .map(Enum::name)
                .toList());
        assertThat(permissoes("Operador")).noneMatch(p -> p.startsWith("FUNCIONARIOS_"));
    }

    @Test
    void moduloDePericiasEntraNoAdministradorENoOperador() {
        migrarAte("26");

        assertThat(permissoes("Administrador"))
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(Permissao.values()).map(Enum::name).toList());
        assertThat(permissoes("Operador"))
                .containsAll(PERMISSOES_V26.stream().map(Enum::name).toList())
                .noneMatch(p -> p.startsWith("FUNCIONARIOS_"));
    }

    private Long inserirUsuario(String username, String perfil) {
        return jdbc.queryForObject(("insert into %s.usuario (username, nome, senha_hash, perfil, criado_em, atualizado_em) "
                + "values (?, ?, 'hash', ?, now(), now()) returning id").formatted(ESQUEMA),
                Long.class, username, "Nome " + username, perfil);
    }

    private String nomeDoPerfil(Long usuarioId) {
        return jdbc.queryForObject(("select p.nome from %s.usuario u join %s.perfil p on p.id = u.perfil_id "
                + "where u.id = ?").formatted(ESQUEMA, ESQUEMA), String.class, usuarioId);
    }

    private List<String> permissoes(String perfil) {
        return jdbc.queryForList(("select pp.permissao from %s.perfil_permissao pp join %s.perfil p on p.id = pp.perfil_id "
                + "where p.nome = ?").formatted(ESQUEMA, ESQUEMA), String.class, perfil);
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
