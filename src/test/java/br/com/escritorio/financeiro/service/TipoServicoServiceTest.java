package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.TipoServicoForm;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoServicoServiceTest {

    @Mock
    private TipoServicoRepository tipoServicoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private TipoServicoService service;

    @BeforeEach
    void setUp() {
        service = new TipoServicoService(tipoServicoRepository, auditoriaService);
    }

    @Test
    void criaComNomeSemEspacosEDescricaoVaziaComoNula() {
        TipoServico criado = service.criar(form("  Pensão por morte  ", "   "));

        assertThat(criado.getNome()).isEqualTo("Pensão por morte");
        assertThat(criado.getDescricao()).isNull();
        assertThat(criado.isAtivo()).isTrue();
        verify(tipoServicoRepository).save(criado);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(TipoServicoService.ENTIDADE), any(),
                contains("Pensão por morte"));
    }

    @Test
    void rejeitaNomeDuplicado() {
        when(tipoServicoRepository.existsByNomeIgnoreCase("salário-maternidade")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(form("salário-maternidade", null)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("nome");
        verify(tipoServicoRepository, never()).save(any());
    }

    @Test
    void rejeitaRenomearParaNomeDeOutroTipo() {
        when(tipoServicoRepository.findById(1L)).thenReturn(Optional.of(new TipoServico("BPC", null)));
        when(tipoServicoRepository.existsByNomeIgnoreCaseAndIdNot("Outros serviços", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, form("Outros serviços", null)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("nome");
    }

    @Test
    void renomearRegistraNomeAnteriorENovoNaAuditoria() {
        TipoServico tipo = new TipoServico("BPC", null);
        when(tipoServicoRepository.findById(1L)).thenReturn(Optional.of(tipo));

        service.atualizar(1L, form("BPC/LOAS", null));

        assertThat(tipo.getNome()).isEqualTo("BPC/LOAS");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(TipoServicoService.ENTIDADE), any(),
                eq("Tipo de serviço renomeado de 'BPC' para 'BPC/LOAS'."));
    }

    @Test
    void salvarSemMudancasNaoGeraAuditoria() {
        when(tipoServicoRepository.findById(1L)).thenReturn(Optional.of(new TipoServico("BPC", "Benefício")));

        service.atualizar(1L, form("BPC", "Benefício"));

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void desativarMantemORegistroERegistraAuditoria() {
        TipoServico tipo = new TipoServico("BPC", null);
        when(tipoServicoRepository.findById(1L)).thenReturn(Optional.of(tipo));

        service.alterarSituacao(1L, false);

        assertThat(tipo.isAtivo()).isFalse();
        verify(tipoServicoRepository, never()).delete(any());
        verify(auditoriaService).registrar(eq(AcaoAuditoria.DESATIVACAO), eq(TipoServicoService.ENTIDADE), any(),
                contains("desativado"));
    }

    @Test
    void tipoInexistenteGeraRegistroNaoEncontrado() {
        when(tipoServicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(RegistroNaoEncontradoException.class);
    }

    private TipoServicoForm form(String nome, String descricao) {
        TipoServicoForm form = new TipoServicoForm();
        form.setNome(nome);
        form.setDescricao(descricao);
        return form;
    }
}
