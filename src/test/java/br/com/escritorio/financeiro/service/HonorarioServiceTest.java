package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.NegociacaoHonorario;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoParcela;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.NegociacaoForm;
import br.com.escritorio.financeiro.dto.ParcelaNegociacaoItem;
import br.com.escritorio.financeiro.repository.NegociacaoHonorarioRepository;
import br.com.escritorio.financeiro.repository.ParcelaHonorarioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HonorarioServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 19);

    @Mock
    private PagamentoService pagamentoService;
    @Mock
    private NegociacaoHonorarioRepository negociacaoRepository;
    @Mock
    private ParcelaHonorarioRepository parcelaRepository;
    @Mock
    private RecebimentoHonorarioRepository recebimentoRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private HonorarioService service;
    private PagamentoAgendado pagamento;

    @BeforeEach
    void setUp() {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        Clock relogio = Clock.fixed(HOJE.atStartOfDay(fuso).toInstant(), fuso);
        service = new HonorarioService(pagamentoService, negociacaoRepository, parcelaRepository,
                recebimentoRepository, auditoriaService, relogio);
        pagamento = pagamento();
        when(pagamentoService.buscar(1L)).thenReturn(pagamento);
        lenient().when(auditoriaService.usuarioAtual()).thenReturn("ana");
        lenient().when(recebimentoRepository.totalRecebidoDoPagamento(1L)).thenReturn(BigDecimal.ZERO);
        lenient().when(negociacaoRepository.findByPagamentoIdAndSituacao(eq(1L), any())).thenReturn(Optional.empty());
        AtomicLong ids = new AtomicLong(10);
        lenient().when(negociacaoRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), ids.getAndIncrement()));
        lenient().when(parcelaRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), ids.getAndIncrement()));
    }

    @Test
    void negociarGeraParcelasIguaisECalculaDesconto() {
        NegociacaoHonorario negociacao = service.negociar(1L, form("400.00", 3, HOJE, "Desconto combinado com o cliente"));

        assertThat(negociacao.getValorOriginal()).isEqualByComparingTo("500.00");
        assertThat(negociacao.getValorNegociado()).isEqualByComparingTo("400.00");
        assertThat(negociacao.getDesconto()).isEqualByComparingTo("100.00");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.NEGOCIADO);
        verify(parcelaRepository, org.mockito.Mockito.times(3)).save(any(ParcelaHonorario.class));
        verify(auditoriaService).registrar(eq(br.com.escritorio.financeiro.domain.AcaoAuditoria.CRIACAO),
                eq(PagamentoService.ENTIDADE), eq(1L), contains("Negociação"));
    }

    @Test
    void recusaSomaDeParcelasDiferenteDoValorNegociado() {
        NegociacaoForm form = form("400.00", 2, HOJE, "Desconto combinado com o cliente");
        form.setParcelas(List.of(item(HOJE, "100.00"), item(HOJE.plusMonths(1), "100.00")));

        assertThatThrownBy(() -> service.negociar(1L, form))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("soma das parcelas");
        verify(negociacaoRepository, never()).save(any());
    }

    @Test
    void recusaNegociarSemHonorarioLancado() {
        pagamento.setValorHonorario(null);

        assertThatThrownBy(() -> service.negociar(1L, form("400.00", 1, HOJE, "Desconto combinado com o cliente")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Lance o valor");
    }

    @Test
    void renegociaCancelandoParcelasAbertasEMantendoRecebido() {
        NegociacaoHonorario anterior = new NegociacaoHonorario(pagamento, new BigDecimal("500.00"),
                new BigDecimal("450.00"), "Primeiro acordo", HOJE.minusDays(5), "ana");
        comId(anterior, 20L);
        ParcelaHonorario aberta = new ParcelaHonorario(anterior, 1, HOJE.plusDays(5), new BigDecimal("450.00"));
        comId(aberta, 21L);
        when(negociacaoRepository.findByPagamentoIdAndSituacao(eq(1L), any())).thenReturn(Optional.of(anterior));
        when(parcelaRepository.findByNegociacaoIdOrderByNumeroAsc(20L)).thenReturn(List.of(aberta));
        when(recebimentoRepository.totalRecebidoDoPagamento(1L)).thenReturn(new BigDecimal("150.00"));

        service.negociar(1L, form("400.00", 1, HOJE, "Novo acordo após conversa"));

        assertThat(anterior.getSituacao().name()).isEqualTo("SUBSTITUIDA");
        assertThat(aberta.getSituacao()).isEqualTo(SituacaoParcela.CANCELADA);
        verify(negociacaoRepository).flush();
    }

    private NegociacaoForm form(String valor, int quantidade, LocalDate vencimento, String motivo) {
        NegociacaoForm form = new NegociacaoForm();
        form.setValorNegociado(new BigDecimal(valor));
        form.setQuantidade(quantidade);
        form.setPrimeiroVencimento(vencimento);
        form.setMotivo(motivo);
        return form;
    }

    private static ParcelaNegociacaoItem item(LocalDate vencimento, String valor) {
        ParcelaNegociacaoItem item = new ParcelaNegociacaoItem();
        item.setVencimento(vencimento);
        item.setValor(new BigDecimal(valor));
        return item;
    }

    private PagamentoAgendado pagamento() {
        PagamentoAgendado p = new PagamentoAgendado(new Cliente("Maria", HOJE), List.of(new Responsavel("Ana", null)),
                new TipoServico("Benefício", null), HOJE, new BigDecimal("1500.00"), "Banco");
        ReflectionTestUtils.setField(p, "id", 1L);
        p.setValorHonorario(new BigDecimal("500.00"));
        return p;
    }

    private static <T> T comId(T entidade, long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
