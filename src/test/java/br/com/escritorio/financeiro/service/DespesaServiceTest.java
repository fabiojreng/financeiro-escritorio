package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import br.com.escritorio.financeiro.domain.Despesa;
import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import br.com.escritorio.financeiro.domain.SituacaoDespesa;
import br.com.escritorio.financeiro.dto.DespesaForm;
import br.com.escritorio.financeiro.repository.CategoriaDespesaRepository;
import br.com.escritorio.financeiro.repository.DespesaRepository;
import br.com.escritorio.financeiro.repository.FormaPagamentoDespesaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DespesaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 10);

    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private CategoriaDespesaRepository categoriaRepository;
    @Mock
    private FormaPagamentoDespesaRepository formaPagamentoRepository;
    @Mock
    private FechamentoDespesaService fechamentoService;
    @Mock
    private AuditoriaService auditoriaService;

    private DespesaService service;
    private CategoriaDespesa energia;
    private FormaPagamentoDespesa bb;

    @BeforeEach
    void setUp() {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        service = new DespesaService(despesaRepository, categoriaRepository, formaPagamentoRepository,
                fechamentoService, auditoriaService, Clock.fixed(HOJE.atStartOfDay(fuso).toInstant(), fuso));
        energia = comId(new CategoriaDespesa("Energia Eletrica", "Despesa c/ Energia, ref."), 1L);
        bb = comId(new FormaPagamentoDespesa("BB", "Banco do Brasil"), 2L);
    }

    @Test
    void lancamentoGuardaOsDadosDaPlanilhaEAudita() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(energia));
        when(formaPagamentoRepository.findById(2L)).thenReturn(Optional.of(bb));

        Despesa despesa = service.criar(form(HOJE.minusDays(2), "  Companhia de Energia ", "318.40"));

        assertThat(despesa.getValor()).isEqualByComparingTo("318.40");
        assertThat(despesa.getFornecedor()).isEqualTo("Companhia de Energia");
        assertThat(despesa.getCategoria()).isSameAs(energia);
        assertThat(despesa.getFormaPagamento()).isSameAs(bb);
        assertThat(despesa.getSituacao()).isEqualTo(SituacaoDespesa.ATIVA);
        assertThat(despesa.getNumeroDocumento()).isNull();
        verify(fechamentoService).exigirAberto(YearMonth.of(2026, 10), "dataPagamento");
        verify(despesaRepository).save(despesa);
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(DespesaService.ENTIDADE), any(),
                descricao.capture());
        assertThat(descricao.getValue()).contains("R$ 318,40", "Companhia de Energia", "Energia Eletrica", "BB");
    }

    @Test
    void dataDePagamentoNoFuturoERecusada() {
        assertThatThrownBy(() -> service.criar(form(HOJE.plusDays(1), "Companhia", "10.00")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("dataPagamento");
        verify(despesaRepository, never()).save(any());
    }

    @Test
    void mesJaEnviadoNaoRecebeNovaDespesa() {
        doThrow(new RegraNegocioException("dataPagamento", "já foram enviadas"))
                .when(fechamentoService).exigirAberto(YearMonth.of(2026, 9), "dataPagamento");

        assertThatThrownBy(() -> service.criar(form(LocalDate.of(2026, 9, 30), "Companhia", "10.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("enviadas");
        verify(despesaRepository, never()).save(any());
    }

    @Test
    void categoriaInativaNaoEntraEmNovaDespesa() {
        energia.setAtivo(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(energia));

        assertThatThrownBy(() -> service.criar(form(HOJE, "Companhia", "10.00")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("categoriaId");
    }

    @Test
    void alteracaoRegistraOQueMudou() {
        Despesa despesa = existente();

        service.atualizar(30L, form(HOJE, "Companhia de Energia", "350.00"));

        assertThat(despesa.getValor()).isEqualByComparingTo("350.00");
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(DespesaService.ENTIDADE), eq(30L),
                descricao.capture());
        assertThat(descricao.getValue()).contains("valor de R$ 318,40 para R$ 350,00");
    }

    @Test
    void despesaDeMesEnviadoNaoPodeSerAlteradaNemMovidaParaEle() {
        Despesa despesa = existente();
        lenient().doThrow(new RegraNegocioException("dataPagamento", "já foram enviadas"))
                .when(fechamentoService).exigirAberto(YearMonth.of(2026, 9), "dataPagamento");

        assertThatThrownBy(() -> service.atualizar(30L, form(LocalDate.of(2026, 9, 15), "Companhia de Energia", "318.40")))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(despesa.getDataPagamento()).isEqualTo(HOJE);

        doThrow(new RegraNegocioException("já foram enviadas"))
                .when(fechamentoService).exigirAberto(YearMonth.of(2026, 10), null);
        assertThatThrownBy(() -> service.atualizar(30L, form(HOJE, "Outro", "318.40")))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(despesa.getFornecedor()).isEqualTo("Companhia de Energia");
    }

    @Test
    void cancelamentoExigeMotivoENaoApaga() {
        Despesa despesa = existente();

        assertThatThrownBy(() -> service.cancelar(30L, "  "))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("motivo");
        assertThat(despesa.isAtiva()).isTrue();

        when(auditoriaService.usuarioAtual()).thenReturn("ana");
        service.cancelar(30L, "Lançada em duplicidade");

        assertThat(despesa.isCancelada()).isTrue();
        assertThat(despesa.getCanceladoPor()).isEqualTo("ana");
        verify(fechamentoService).exigirAberto(eq(YearMonth.of(2026, 10)), isNull());
        verify(despesaRepository, never()).delete(any());
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CANCELAMENTO), eq(DespesaService.ENTIDADE), eq(30L),
                any());
    }

    @Test
    void despesaCanceladaNaoEAlterada() {
        Despesa despesa = existente();
        despesa.cancelar("Duplicada", "ana", HOJE.atStartOfDay());

        assertThatThrownBy(() -> service.atualizar(30L, form(HOJE, "Outro", "10.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("cancelada");
    }

    private Despesa existente() {
        Despesa despesa = comId(new Despesa(HOJE, energia, bb, "Despesa c/ Energia, ref.", new BigDecimal("318.40"),
                "Companhia de Energia"), 30L);
        when(despesaRepository.findComDetalhesById(30L)).thenReturn(Optional.of(despesa));
        return despesa;
    }

    private static DespesaForm form(LocalDate data, String fornecedor, String valor) {
        DespesaForm form = new DespesaForm();
        form.setDataPagamento(data);
        form.setCategoriaId(1L);
        form.setFormaPagamentoId(2L);
        form.setHistorico("Despesa c/ Energia, ref.");
        form.setFornecedor(fornecedor);
        form.setValor(new BigDecimal(valor));
        return form;
    }

    private static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
