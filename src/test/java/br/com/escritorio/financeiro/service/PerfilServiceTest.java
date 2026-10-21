package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.PerfisDeTeste;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.dto.PerfilForm;
import br.com.escritorio.financeiro.repository.PerfilRepository;
import br.com.escritorio.financeiro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static br.com.escritorio.financeiro.domain.Permissao.ADMIN_PERFIS;
import static br.com.escritorio.financeiro.domain.Permissao.ADMIN_USUARIOS;
import static br.com.escritorio.financeiro.domain.Permissao.AGENDA_EDITAR;
import static br.com.escritorio.financeiro.domain.Permissao.AGENDA_VISUALIZAR;
import static br.com.escritorio.financeiro.domain.Permissao.CLIENTES_VISUALIZAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private PerfilService service;

    @BeforeEach
    void setUp() {
        service = new PerfilService(perfilRepository, usuarioRepository, auditoriaService);
    }

    @Test
    void criaPerfilERegistraAsPermissoesNaAuditoria() {
        Perfil criado = service.criar(form(" Recepção ", AGENDA_VISUALIZAR, CLIENTES_VISUALIZAR));

        verify(perfilRepository).save(criado);
        assertThat(criado.getNome()).isEqualTo("Recepção");
        assertThat(criado.getPermissoes()).containsExactlyInAnyOrder(AGENDA_VISUALIZAR, CLIENTES_VISUALIZAR);
        assertThat(auditoria(AcaoAuditoria.CRIACAO))
                .contains("Perfil 'Recepção' cadastrado", "Visualizar clientes", "Visualizar a agenda");
    }

    @Test
    void rejeitaNomeRepetido() {
        when(perfilRepository.existsByNomeIgnoreCase("operador")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(form("operador", AGENDA_VISUALIZAR)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("nome");
        verify(perfilRepository, never()).save(any());
    }

    @Test
    void duplicacaoCopiaAsPermissoesERegistraOPerfilDeOrigem() {
        Perfil operador = PerfisDeTeste.operador();
        when(perfilRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
        PerfilForm copia = PerfilForm.copiaDe(operador);
        assertThat(copia.getNome()).isEqualTo("Cópia de Operador");
        copia.setNome("Operador júnior");

        Perfil criado = service.criar(copia);

        assertThat(criado.getPermissoes()).isEqualTo(operador.getPermissoes());
        assertThat(auditoria(AcaoAuditoria.DUPLICACAO))
                .contains("Perfil 'Operador júnior' criado a partir do perfil 'Operador'");
    }

    @Test
    void perfilDoSistemaSoTemADescricaoAlteradaEContinuaComTodasAsPermissoes() {
        Perfil administrador = PerfisDeTeste.administrador();
        when(perfilRepository.findById(1L)).thenReturn(Optional.of(administrador));
        PerfilForm form = form("Outro nome", AGENDA_VISUALIZAR);
        form.setDescricao("Acesso total");

        service.atualizar(1L, form, "chefe");

        assertThat(administrador.getNome()).isEqualTo("Administrador");
        assertThat(administrador.getDescricao()).isEqualTo("Acesso total");
        assertThat(administrador.permissoesEfetivas()).containsExactlyInAnyOrder(Permissao.values());
        assertThat(auditoria(AcaoAuditoria.ALTERACAO)).contains("descrição");
    }

    @Test
    void naoInativaOPerfilDoSistema() {
        Perfil administrador = PerfisDeTeste.administrador();
        when(perfilRepository.findById(1L)).thenReturn(Optional.of(administrador));

        assertThatThrownBy(() -> service.alterarSituacao(1L, false))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("é do sistema");
        assertThat(administrador.isAtivo()).isTrue();
    }

    @Test
    void naoInativaPerfilComUsuariosAtivos() {
        Perfil operador = PerfisDeTeste.operador();
        when(perfilRepository.findById(2L)).thenReturn(Optional.of(operador));
        when(usuarioRepository.countByPerfilAndAtivoTrue(operador)).thenReturn(2L);

        assertThatThrownBy(() -> service.alterarSituacao(2L, false))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("2 usuários ativos");
        assertThat(operador.isAtivo()).isTrue();
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void inativaPerfilSemUsuariosAtivosERegistraAuditoria() {
        Perfil perfil = PerfisDeTeste.perfil(5L, "Estagiário", List.of(AGENDA_VISUALIZAR));
        when(perfilRepository.findById(5L)).thenReturn(Optional.of(perfil));
        when(usuarioRepository.countByPerfilAndAtivoTrue(perfil)).thenReturn(0L);

        service.alterarSituacao(5L, false);

        assertThat(perfil.isAtivo()).isFalse();
        assertThat(auditoria(AcaoAuditoria.DESATIVACAO)).contains("Perfil 'Estagiário' inativado");
    }

    @Test
    void naoPermiteTirarDoProprioPerfilAPermissaoDeGerenciarPerfis() {
        Perfil gestor = PerfisDeTeste.perfil(5L, "Gestor", List.of(ADMIN_PERFIS, AGENDA_VISUALIZAR));
        when(perfilRepository.findById(5L)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.findByUsernameIgnoreCase("gi"))
                .thenReturn(Optional.of(new Usuario("gi", "Gisele", "hash", gestor)));

        assertThatThrownBy(() -> service.atualizar(5L, form("Gestor", AGENDA_VISUALIZAR), "gi"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("seu próprio perfil")
                .extracting("campo").isEqualTo("permissoes");
        assertThat(gestor.getPermissoes()).contains(ADMIN_PERFIS);
    }

    @Test
    void permiteTirarGerenciarPerfisDoPerfilDeOutrasPessoas() {
        Perfil gestor = PerfisDeTeste.perfil(5L, "Gestor", List.of(ADMIN_PERFIS, AGENDA_VISUALIZAR));
        when(perfilRepository.findById(5L)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.findByUsernameIgnoreCase("chefe"))
                .thenReturn(Optional.of(new Usuario("chefe", "Chefe", "hash", PerfisDeTeste.administrador())));

        service.atualizar(5L, form("Gestor", AGENDA_VISUALIZAR), "chefe");

        assertThat(gestor.getPermissoes()).containsExactly(AGENDA_VISUALIZAR);
    }

    @Test
    void naoPermiteTirarGerenciarUsuariosDoUnicoPerfilQueAindaTemQuemGerencie() {
        Perfil gestor = PerfisDeTeste.perfil(5L, "Gestor", List.of(ADMIN_USUARIOS, AGENDA_VISUALIZAR));
        when(perfilRepository.findById(5L)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.countByPerfilAndAtivoTrue(gestor)).thenReturn(1L);
        when(usuarioRepository.contarAtivosComPermissaoForaDoPerfil(ADMIN_USUARIOS, 5L)).thenReturn(0L);

        assertThatThrownBy(() -> service.atualizar(5L, form("Gestor", AGENDA_VISUALIZAR), "chefe"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("gerenciar usuários");
        assertThat(gestor.getPermissoes()).contains(ADMIN_USUARIOS);
    }

    @Test
    void permiteTirarGerenciarUsuariosQuandoOutroUsuarioAtivoContinuaPodendo() {
        Perfil gestor = PerfisDeTeste.perfil(5L, "Gestor", List.of(ADMIN_USUARIOS, AGENDA_VISUALIZAR));
        when(perfilRepository.findById(5L)).thenReturn(Optional.of(gestor));
        when(usuarioRepository.countByPerfilAndAtivoTrue(gestor)).thenReturn(1L);
        when(usuarioRepository.contarAtivosComPermissaoForaDoPerfil(ADMIN_USUARIOS, 5L)).thenReturn(1L);

        service.atualizar(5L, form("Gestor", AGENDA_VISUALIZAR), "chefe");

        assertThat(gestor.getPermissoes()).containsExactly(AGENDA_VISUALIZAR);
    }

    @Test
    void auditoriaDaEdicaoListaPermissoesAdicionadasERemovidas() {
        Perfil recepcao = PerfisDeTeste.perfil(5L, "Recepção", List.of(AGENDA_VISUALIZAR, CLIENTES_VISUALIZAR));
        when(perfilRepository.findById(5L)).thenReturn(Optional.of(recepcao));

        service.atualizar(5L, form("Balcão", AGENDA_VISUALIZAR, AGENDA_EDITAR), "chefe");

        assertThat(recepcao.getNome()).isEqualTo("Balcão");
        assertThat(recepcao.getPermissoes()).containsExactlyInAnyOrder(AGENDA_VISUALIZAR, AGENDA_EDITAR);
        assertThat(auditoria(AcaoAuditoria.ALTERACAO)).contains(
                "nome de 'Recepção' para 'Balcão'",
                "permissões adicionadas: Lançar e editar pagamentos",
                "permissões removidas: Visualizar clientes");
    }

    @Test
    void edicaoSemMudancaNaoGeraAuditoria() {
        Perfil recepcao = PerfisDeTeste.perfil(5L, "Recepção", List.of(AGENDA_VISUALIZAR));
        when(perfilRepository.findById(5L)).thenReturn(Optional.of(recepcao));

        service.atualizar(5L, form("Recepção", AGENDA_VISUALIZAR), "chefe");

        verify(auditoriaService, never()).registrar(any(), anyString(), any(), anyString());
    }

    private String auditoria(AcaoAuditoria acao) {
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(acao), eq(PerfilService.ENTIDADE), any(), descricao.capture());
        return descricao.getValue();
    }

    private static PerfilForm form(String nome, Permissao... permissoes) {
        PerfilForm form = new PerfilForm();
        form.setNome(nome);
        Set<Permissao> selecionadas = EnumSet.noneOf(Permissao.class);
        selecionadas.addAll(List.of(permissoes));
        form.setPermissoes(selecionadas);
        return form;
    }
}
