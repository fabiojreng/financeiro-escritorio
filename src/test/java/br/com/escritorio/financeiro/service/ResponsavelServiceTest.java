package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.dto.ResponsavelForm;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
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
class ResponsavelServiceTest {

    @Mock
    private ResponsavelRepository responsavelRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private ResponsavelService service;

    @BeforeEach
    void setUp() {
        service = new ResponsavelService(responsavelRepository, auditoriaService);
    }

    @Test
    void criaResponsavelAtivoERegistraAuditoria() {
        Responsavel criado = service.criar(form("  Ana Paula  ", ""));

        assertThat(criado.getNome()).isEqualTo("Ana Paula");
        assertThat(criado.getObservacoes()).isNull();
        assertThat(criado.isAtivo()).isTrue();
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(ResponsavelService.ENTIDADE), any(),
                contains("Ana Paula"));
    }

    @Test
    void rejeitaNomeDuplicado() {
        when(responsavelRepository.existsByNomeIgnoreCase("ana paula")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(form("ana paula", null)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("nome");
        verify(responsavelRepository, never()).save(any());
    }

    @Test
    void renomearPreservaORegistroERegistraNomes() {
        Responsavel responsavel = new Responsavel("Ana", null);
        when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavel));

        service.atualizar(1L, form("Ana Paula", null));

        assertThat(responsavel.getNome()).isEqualTo("Ana Paula");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(ResponsavelService.ENTIDADE), any(),
                eq("Responsável renomeado de 'Ana' para 'Ana Paula'."));
    }

    @Test
    void salvarSemMudancasNaoGeraAuditoria() {
        when(responsavelRepository.findById(1L)).thenReturn(Optional.of(new Responsavel("Ana", "Manhãs")));

        service.atualizar(1L, form("Ana", "Manhãs"));

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void desativarNaoApagaERegistraAuditoria() {
        Responsavel responsavel = new Responsavel("Ana", null);
        when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavel));

        service.alterarSituacao(1L, false);

        assertThat(responsavel.isAtivo()).isFalse();
        verify(responsavelRepository, never()).delete(any());
        verify(auditoriaService).registrar(eq(AcaoAuditoria.DESATIVACAO), eq(ResponsavelService.ENTIDADE), any(),
                contains("desativado"));
    }

    private ResponsavelForm form(String nome, String observacoes) {
        ResponsavelForm form = new ResponsavelForm();
        form.setNome(nome);
        form.setObservacoes(observacoes);
        return form;
    }
}
