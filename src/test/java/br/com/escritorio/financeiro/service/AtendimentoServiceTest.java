package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.AtendimentoForm;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtendimentoServiceTest {

    @Mock
    private AtendimentoRepository atendimentoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TipoServicoRepository tipoServicoRepository;
    @Mock
    private ResponsavelRepository responsavelRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private AtendimentoService service;

    private Cliente cliente;
    private TipoServico tipo;
    private Responsavel responsavel;

    @BeforeEach
    void setUp() {
        service = new AtendimentoService(atendimentoRepository, clienteRepository, tipoServicoRepository,
                responsavelRepository, auditoriaService);
        cliente = comId(new Cliente("Maria", LocalDate.of(2026, 1, 10)), 1L);
        tipo = comId(new TipoServico("Salário-maternidade", null), 10L);
        responsavel = comId(new Responsavel("Ana", null), 20L);
    }

    @Test
    void abreAtendimentoComCadastrosAtivosERegistraAuditoria() {
        quandoCadastrosExistem();

        Atendimento atendimento = service.criar(form(1L, 10L, 20L));

        assertThat(atendimento.getCliente()).isSameAs(cliente);
        assertThat(atendimento.getTipoServico()).isSameAs(tipo);
        assertThat(atendimento.getResponsavel()).isSameAs(responsavel);
        assertThat(atendimento.getSituacao()).isEqualTo(SituacaoAtendimento.ABERTO);
        verify(atendimentoRepository).save(atendimento);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(AtendimentoService.ENTIDADE), any(),
                contains("Maria"));
    }

    @Test
    void rejeitaClienteInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(form(99L, 10L, 20L)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("clienteId");
        verify(atendimentoRepository, never()).save(any());
    }

    @Test
    void rejeitaClienteInativo() {
        cliente.setAtivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> service.criar(form(1L, 10L, 20L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void rejeitaTipoDeServicoInexistente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(tipoServicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(form(1L, 99L, 20L)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("tipoServicoId");
    }

    @Test
    void rejeitaTipoDeServicoInativoEmNovoAtendimento() {
        tipo.setAtivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(tipoServicoRepository.findById(10L)).thenReturn(Optional.of(tipo));

        assertThatThrownBy(() -> service.criar(form(1L, 10L, 20L)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("tipoServicoId");
        verify(atendimentoRepository, never()).save(any());
    }

    @Test
    void rejeitaResponsavelInativoEmNovoAtendimento() {
        responsavel.setAtivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(tipoServicoRepository.findById(10L)).thenReturn(Optional.of(tipo));
        when(responsavelRepository.findById(20L)).thenReturn(Optional.of(responsavel));

        assertThatThrownBy(() -> service.criar(form(1L, 10L, 20L)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("responsavelId");
    }

    @Test
    void edicaoPodeManterTipoEResponsavelQueForamDesativados() {
        Atendimento existente = comId(new Atendimento(cliente, tipo, responsavel, LocalDate.of(2026, 2, 1)), 5L);
        tipo.setAtivo(false);
        responsavel.setAtivo(false);
        when(atendimentoRepository.findComDetalhesById(5L)).thenReturn(Optional.of(existente));

        AtendimentoForm form = form(1L, 10L, 20L);
        form.setDataAbertura(LocalDate.of(2026, 2, 1));
        form.setSituacao(SituacaoAtendimento.EM_ANDAMENTO);
        service.atualizar(5L, form);

        assertThat(existente.getSituacao()).isEqualTo(SituacaoAtendimento.EM_ANDAMENTO);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(AtendimentoService.ENTIDADE), eq(5L),
                contains("situação de Aberto para Em andamento"));
    }

    @Test
    void edicaoNaoPodeTrocarParaResponsavelInativo() {
        Atendimento existente = comId(new Atendimento(cliente, tipo, responsavel, LocalDate.of(2026, 2, 1)), 5L);
        Responsavel inativo = comId(new Responsavel("Bruno", null), 21L);
        inativo.setAtivo(false);
        when(atendimentoRepository.findComDetalhesById(5L)).thenReturn(Optional.of(existente));
        when(responsavelRepository.findById(21L)).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> service.atualizar(5L, form(1L, 10L, 21L)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("responsavelId");
        assertThat(existente.getResponsavel()).isSameAs(responsavel);
    }

    private void quandoCadastrosExistem() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(tipoServicoRepository.findById(10L)).thenReturn(Optional.of(tipo));
        when(responsavelRepository.findById(20L)).thenReturn(Optional.of(responsavel));
    }

    private AtendimentoForm form(Long clienteId, Long tipoId, Long responsavelId) {
        AtendimentoForm form = new AtendimentoForm();
        form.setClienteId(clienteId);
        form.setTipoServicoId(tipoId);
        form.setResponsavelId(responsavelId);
        form.setDataAbertura(LocalDate.of(2026, 3, 1));
        return form;
    }

    private static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
