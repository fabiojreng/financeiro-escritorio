package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.FormaCobranca;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoParcela;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.AcordoForm;
import br.com.escritorio.financeiro.dto.GerarParcelasForm;
import br.com.escritorio.financeiro.dto.RecebimentoForm;
import br.com.escritorio.financeiro.repository.AcordoHonorariosRepository;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.ParcelaHonorarioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
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
import java.time.ZoneId;
import java.util.List;
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
class HonorariosServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 10);

    @Mock
    private AcordoHonorariosRepository acordoRepository;
    @Mock
    private ParcelaHonorarioRepository parcelaRepository;
    @Mock
    private RecebimentoHonorarioRepository recebimentoRepository;
    @Mock
    private AtendimentoRepository atendimentoRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private HonorariosService service;
    private Atendimento atendimento;

    @BeforeEach
    void setUp() {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        service = new HonorariosService(acordoRepository, parcelaRepository, recebimentoRepository,
                atendimentoRepository, auditoriaService, Clock.fixed(HOJE.atStartOfDay(fuso).toInstant(), fuso));
        atendimento = comId(new Atendimento(new Cliente("Maria", HOJE), new TipoServico("Aposentadoria", null),
                new Responsavel("Ana", null), HOJE), 3L);
    }

    @Test
    void valorFixoExigeValorContratado() {
        quandoAtendimentoSemAcordo();

        assertThatThrownBy(() -> service.criarAcordo(3L, acordoForm(FormaCobranca.VALOR_FIXO, null)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("valorContratado");
    }

    @Test
    void percentualExigePercentualEBaseDeCalculo() {
        quandoAtendimentoSemAcordo();
        AcordoForm form = acordoForm(FormaCobranca.PERCENTUAL, null);
        form.setPercentual(new BigDecimal("30"));

        assertThatThrownBy(() -> service.criarAcordo(3L, form))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("baseCalculo");
    }

    @Test
    void acordoPercentualNaoPreencheValorContratadoSozinho() {
        quandoAtendimentoSemAcordo();
        AcordoForm form = acordoForm(FormaCobranca.PERCENTUAL, null);
        form.setPercentual(new BigDecimal("30"));
        form.setBaseCalculo("Valores retroativos do benefício");

        AcordoHonorarios acordo = service.criarAcordo(3L, form);

        assertThat(acordo.getValorContratado()).isNull();
        assertThat(acordo.getPercentual()).isEqualByComparingTo("30");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(HonorariosService.ENTIDADE_ACORDO), any(),
                contains("valor contratado a definir"));
    }

    @Test
    void valorFixoDescartaPercentualEBase() {
        quandoAtendimentoSemAcordo();
        AcordoForm form = acordoForm(FormaCobranca.VALOR_FIXO, "3000.00");
        form.setPercentual(new BigDecimal("30"));
        form.setBaseCalculo("Retroativos");

        AcordoHonorarios acordo = service.criarAcordo(3L, form);

        assertThat(acordo.getPercentual()).isNull();
        assertThat(acordo.getBaseCalculo()).isNull();
    }

    @Test
    void atendimentoSoTemUmAcordo() {
        when(atendimentoRepository.findComDetalhesById(3L)).thenReturn(Optional.of(atendimento));
        when(acordoRepository.existsByAtendimentoId(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.criarAcordo(3L, acordoForm(FormaCobranca.VALOR_FIXO, "100.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já possui");
        verify(acordoRepository, never()).save(any());
    }

    @Test
    void valorContratadoNaoPodeFicarAbaixoDoRecebido() {
        AcordoHonorarios acordo = acordoExistente("3000.00");
        when(recebimentoRepository.totalRecebidoNoAcordo(10L)).thenReturn(new BigDecimal("1500.00"));

        assertThatThrownBy(() -> service.atualizarAcordo(10L, acordoForm(FormaCobranca.VALOR_FIXO, "1000.00")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("valorContratado");
        assertThat(acordo.getValorContratado()).isEqualByComparingTo("3000.00");
    }

    @Test
    void gerarParcelasDivideOValorEmVencimentosMensais() {
        acordoExistente("1000.00");
        GerarParcelasForm form = new GerarParcelasForm();
        form.setQuantidade(3);
        form.setValorTotal(new BigDecimal("1000.00"));
        form.setPrimeiroVencimento(LocalDate.of(2026, 11, 5));

        List<ParcelaHonorario> parcelas = service.gerarParcelas(10L, form);

        assertThat(parcelas).extracting(ParcelaHonorario::getNumero).containsExactly(1, 2, 3);
        assertThat(parcelas).extracting(ParcelaHonorario::getVencimento).containsExactly(
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 12, 5), LocalDate.of(2027, 1, 5));
        assertThat(parcelas.get(2).getValor()).isEqualByComparingTo("333.34");
    }

    @Test
    void naoGeraParcelasSeJaExistemAtivas() {
        acordoExistente("1000.00");
        when(parcelaRepository.existsByAcordoIdAndSituacaoNot(10L, SituacaoParcela.CANCELADA)).thenReturn(true);
        GerarParcelasForm form = new GerarParcelasForm();
        form.setQuantidade(2);
        form.setValorTotal(new BigDecimal("1000.00"));
        form.setPrimeiroVencimento(HOJE);

        assertThatThrownBy(() -> service.gerarParcelas(10L, form)).isInstanceOf(RegraNegocioException.class);
        verify(parcelaRepository, never()).saveAll(any());
    }

    @Test
    void recebimentoAcimaDoSaldoERejeitado() {
        acordoExistente("3000.00");
        when(recebimentoRepository.totalRecebidoNoAcordo(10L)).thenReturn(new BigDecimal("2500.00"));

        assertThatThrownBy(() -> service.registrarRecebimento(10L, recebimentoForm("600.00", null)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("valor");
        verify(recebimentoRepository, never()).save(any());
    }

    @Test
    void recebimentoComDataFuturaERejeitado() {
        acordoExistente("3000.00");
        RecebimentoForm form = recebimentoForm("100.00", null);
        form.setDataRecebimento(HOJE.plusDays(1));

        assertThatThrownBy(() -> service.registrarRecebimento(10L, form))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("dataRecebimento");
    }

    @Test
    void recebimentoQueCompletaAParcelaMarcaComoPaga() {
        AcordoHonorarios acordo = acordoExistente("3000.00");
        ParcelaHonorario parcela = comId(new ParcelaHonorario(acordo, 1, HOJE, new BigDecimal("500.00")), 20L);
        when(recebimentoRepository.totalRecebidoNoAcordo(10L)).thenReturn(new BigDecimal("200.00"));
        when(parcelaRepository.findById(20L)).thenReturn(Optional.of(parcela));
        when(recebimentoRepository.totalRecebidoNaParcela(20L)).thenReturn(new BigDecimal("200.00"));

        RecebimentoHonorario recebimento = service.registrarRecebimento(10L, recebimentoForm("300.00", 20L));

        assertThat(recebimento.getParcela()).isSameAs(parcela);
        assertThat(parcela.getSituacao()).isEqualTo(SituacaoParcela.PAGA);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(HonorariosService.ENTIDADE_RECEBIMENTO),
                any(), contains("parcela 1"));
    }

    @Test
    void recebimentoNaoAceitaParcelaDeOutroAcordo() {
        acordoExistente("3000.00");
        AcordoHonorarios outro = comId(new AcordoHonorarios(atendimento, FormaCobranca.VALOR_FIXO, HOJE), 99L);
        ParcelaHonorario parcela = comId(new ParcelaHonorario(outro, 1, HOJE, new BigDecimal("500.00")), 20L);
        when(recebimentoRepository.totalRecebidoNoAcordo(10L)).thenReturn(BigDecimal.ZERO);
        when(parcelaRepository.findById(20L)).thenReturn(Optional.of(parcela));

        assertThatThrownBy(() -> service.registrarRecebimento(10L, recebimentoForm("100.00", 20L)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("parcelaId");
    }

    @Test
    void cancelarRecebimentoExigeMotivoENaoApagaOLancamento() {
        AcordoHonorarios acordo = acordo("3000.00");
        ParcelaHonorario parcela = comId(new ParcelaHonorario(acordo, 1, HOJE, new BigDecimal("500.00")), 20L);
        parcela.atualizarPagamento(new BigDecimal("500.00"));
        RecebimentoHonorario recebimento = comId(new RecebimentoHonorario(acordo, parcela, HOJE,
                new BigDecimal("500.00"), FormaPagamento.PIX), 30L);
        when(recebimentoRepository.findComDetalhesById(30L)).thenReturn(Optional.of(recebimento));

        assertThatThrownBy(() -> service.cancelarRecebimento(30L, "  "))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("motivo");
        assertThat(recebimento.isCancelado()).isFalse();

        when(auditoriaService.usuarioAtual()).thenReturn("admin");
        when(recebimentoRepository.totalRecebidoNaParcela(20L)).thenReturn(BigDecimal.ZERO);
        service.cancelarRecebimento(30L, "Lançado em duplicidade");

        assertThat(recebimento.isCancelado()).isTrue();
        assertThat(recebimento.getMotivoCancelamento()).isEqualTo("Lançado em duplicidade");
        assertThat(recebimento.getCanceladoPor()).isEqualTo("admin");
        assertThat(parcela.getSituacao()).isEqualTo(SituacaoParcela.ABERTA);
        verify(recebimentoRepository, never()).delete(any());
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CANCELAMENTO), eq(HonorariosService.ENTIDADE_RECEBIMENTO),
                eq(30L), descricao.capture());
        assertThat(descricao.getValue()).contains("R$ 500,00", "Lançado em duplicidade");
    }

    @Test
    void parcelaComRecebimentoNaoPodeSerCancelada() {
        AcordoHonorarios acordo = acordo("3000.00");
        ParcelaHonorario parcela = comId(new ParcelaHonorario(acordo, 1, HOJE, new BigDecimal("500.00")), 20L);
        when(parcelaRepository.findComDetalhesById(20L)).thenReturn(Optional.of(parcela));
        when(recebimentoRepository.totalRecebidoNaParcela(20L)).thenReturn(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.cancelarParcela(20L, "Renegociado com o cliente"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("recebimentos");
        assertThat(parcela.getSituacao()).isEqualTo(SituacaoParcela.ABERTA);
    }

    private void quandoAtendimentoSemAcordo() {
        when(atendimentoRepository.findComDetalhesById(3L)).thenReturn(Optional.of(atendimento));
        when(acordoRepository.existsByAtendimentoId(3L)).thenReturn(false);
    }

    private AcordoHonorarios acordoExistente(String valor) {
        AcordoHonorarios acordo = acordo(valor);
        when(acordoRepository.findComDetalhesById(10L)).thenReturn(Optional.of(acordo));
        return acordo;
    }

    private AcordoHonorarios acordo(String valor) {
        AcordoHonorarios acordo = comId(new AcordoHonorarios(atendimento, FormaCobranca.VALOR_FIXO, HOJE), 10L);
        acordo.setValorContratado(new BigDecimal(valor));
        return acordo;
    }

    private static AcordoForm acordoForm(FormaCobranca forma, String valor) {
        AcordoForm form = new AcordoForm();
        form.setFormaCobranca(forma);
        form.setValorContratado(valor == null ? null : new BigDecimal(valor));
        form.setDataAcordo(HOJE);
        return form;
    }

    private static RecebimentoForm recebimentoForm(String valor, Long parcelaId) {
        RecebimentoForm form = new RecebimentoForm();
        form.setDataRecebimento(HOJE);
        form.setValor(new BigDecimal(valor));
        form.setFormaPagamento(FormaPagamento.PIX);
        form.setParcelaId(parcelaId);
        return form;
    }

    private static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
