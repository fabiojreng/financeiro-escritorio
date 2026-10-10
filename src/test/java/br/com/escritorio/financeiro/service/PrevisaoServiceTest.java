package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.domain.TipoPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.AgendaMensal;
import br.com.escritorio.financeiro.dto.ConfirmacaoLiberacaoForm;
import br.com.escritorio.financeiro.dto.FiltroAgenda;
import br.com.escritorio.financeiro.dto.PrevisaoForm;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.PrevisaoBeneficioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrevisaoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 10);

    @Mock
    private PrevisaoBeneficioRepository previsaoRepository;
    @Mock
    private AtendimentoRepository atendimentoRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private PrevisaoService service;
    private Atendimento atendimento;

    @BeforeEach
    void setUp() {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        Clock relogio = Clock.fixed(HOJE.atStartOfDay(fuso).toInstant(), fuso);
        service = new PrevisaoService(previsaoRepository, atendimentoRepository, auditoriaService, relogio);
        atendimento = comId(new Atendimento(new Cliente("Maria", HOJE), new TipoServico("Salário-maternidade", null),
                new Responsavel("Ana", null), HOJE.minusMonths(2)), 3L);
    }

    @Test
    void cadastraPrevisaoSemValorConfirmadoERegistraAuditoria() {
        when(atendimentoRepository.findComDetalhesById(3L)).thenReturn(Optional.of(atendimento));

        PrevisaoBeneficio previsao = service.criar(3L, form("1500.00", SituacaoPrevisao.PREVISTO));

        assertThat(previsao.getAtendimento()).isSameAs(atendimento);
        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.PREVISTO);
        assertThat(previsao.getValorConfirmado()).isNull();
        assertThat(previsao.getDataEfetiva()).isNull();
        verify(previsaoRepository).save(previsao);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(PrevisaoService.ENTIDADE), any(),
                contains("R$ 1.500,00"));
    }

    @Test
    void rejeitaValorEstimadoNegativo() {
        when(atendimentoRepository.findComDetalhesById(3L)).thenReturn(Optional.of(atendimento));

        assertThatThrownBy(() -> service.criar(3L, form("-1.00", SituacaoPrevisao.PREVISTO)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("valorEstimado");
        verify(previsaoRepository, never()).save(any());
    }

    @Test
    void naoCadastraPrevisaoJaComoLiberada() {
        when(atendimentoRepository.findComDetalhesById(3L)).thenReturn(Optional.of(atendimento));

        assertThatThrownBy(() -> service.criar(3L, form("1500.00", SituacaoPrevisao.LIBERADO)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("situacao");
    }

    @Test
    void naoCadastraPrevisaoEmAtendimentoCancelado() {
        atendimento.setSituacao(SituacaoAtendimento.CANCELADO);
        when(atendimentoRepository.findComDetalhesById(3L)).thenReturn(Optional.of(atendimento));

        assertThatThrownBy(() -> service.criar(3L, form("1500.00", SituacaoPrevisao.PREVISTO)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("cancelado");
    }

    @Test
    void consultarAgendaDepoisDaDataNaoMudaASituacao() {
        PrevisaoBeneficio vencida = previsao(HOJE.minusDays(10), "1500.00");
        when(previsaoRepository.pesquisar(any(), any(), any(), anyString(), isNull(), isNull()))
                .thenReturn(List.of(vencida));
        FiltroAgenda filtro = new FiltroAgenda();
        filtro.setMes(YearMonth.from(HOJE));

        AgendaMensal agenda = service.agenda(filtro);

        assertThat(agenda.previsoes()).containsExactly(vencida);
        assertThat(vencida.getSituacao()).isEqualTo(SituacaoPrevisao.PREVISTO);
        verifyNoInteractions(auditoriaService);
        verify(previsaoRepository, never()).save(any());
    }

    @Test
    void agendaUsaOMesInteiroQuandoNaoHaPeriodo() {
        FiltroAgenda filtro = new FiltroAgenda();
        filtro.setMes(YearMonth.of(2026, 2));

        service.agenda(filtro);

        verify(previsaoRepository).pesquisar(eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28)),
                eq(List.of(SituacaoPrevisao.values())), eq("%%"), isNull(), isNull());
    }

    @Test
    void somenteAtrasadasIgnoraOMesEBuscaPendentesAteOntem() {
        FiltroAgenda filtro = new FiltroAgenda();
        filtro.setMes(YearMonth.of(2026, 12));
        filtro.setAtrasadas(true);
        filtro.setResponsavelId(7L);

        service.agenda(filtro);

        verify(previsaoRepository).pesquisar(eq(LocalDate.of(1900, 1, 1)), eq(HOJE.minusDays(1)),
                eq(SituacaoPrevisao.PENDENTES), eq("%%"), eq(7L), isNull());
    }

    @Test
    void totaisDaAgendaSeparamEstimadoDeConfirmadoEIgnoramCanceladas() {
        PrevisaoBeneficio prevista = previsao(HOJE, "1000.00");
        PrevisaoBeneficio liberada = previsao(HOJE, "2000.00");
        liberada.confirmarLiberacao(HOJE, new BigDecimal("1800.00"));
        PrevisaoBeneficio cancelada = previsao(HOJE, "5000.00");
        cancelada.alterarSituacao(SituacaoPrevisao.CANCELADO);

        AgendaMensal agenda = AgendaMensal.de(List.of(prevista, liberada, cancelada));

        assertThat(agenda.totalEstimado()).isEqualByComparingTo("3000.00");
        assertThat(agenda.totalConfirmado()).isEqualByComparingTo("1800.00");
    }

    @Test
    void confirmarLiberacaoGravaDataEValorSemMexerNoEstimado() {
        PrevisaoBeneficio previsao = comId(previsao(HOJE.minusDays(1), "1500.00"), 9L);
        when(previsaoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(previsao));

        service.confirmarLiberacao(9L, new ConfirmacaoLiberacaoForm(HOJE, new BigDecimal("1480.00")));

        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.LIBERADO);
        assertThat(previsao.getDataEfetiva()).isEqualTo(HOJE);
        assertThat(previsao.getValorConfirmado()).isEqualByComparingTo("1480.00");
        assertThat(previsao.getValorEstimado()).isEqualByComparingTo("1500.00");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CONFIRMACAO), eq(PrevisaoService.ENTIDADE), eq(9L),
                contains("R$ 1.480,00"));
    }

    @Test
    void confirmarLiberacaoRejeitaDataFuturaEValorZero() {
        PrevisaoBeneficio previsao = comId(previsao(HOJE, "1500.00"), 9L);
        when(previsaoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(previsao));

        assertThatThrownBy(() -> service.confirmarLiberacao(9L,
                new ConfirmacaoLiberacaoForm(HOJE.plusDays(1), new BigDecimal("100.00"))))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("dataEfetiva");
        assertThatThrownBy(() -> service.confirmarLiberacao(9L,
                new ConfirmacaoLiberacaoForm(HOJE, BigDecimal.ZERO)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("valorConfirmado");
        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.PREVISTO);
    }

    @Test
    void naoConfirmaPrevisaoCancelada() {
        PrevisaoBeneficio previsao = comId(previsao(HOJE, "1500.00"), 9L);
        previsao.alterarSituacao(SituacaoPrevisao.CANCELADO);
        when(previsaoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(previsao));

        assertThatThrownBy(() -> service.confirmarLiberacao(9L,
                new ConfirmacaoLiberacaoForm(HOJE, new BigDecimal("100.00"))))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.CANCELADO);
    }

    @Test
    void editarValorEstimadoNaoAlteraValorConfirmado() {
        PrevisaoBeneficio previsao = comId(previsao(HOJE.minusDays(3), "1500.00"), 9L);
        previsao.confirmarLiberacao(HOJE.minusDays(1), new BigDecimal("1480.00"));
        when(previsaoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(previsao));

        PrevisaoForm form = PrevisaoForm.de(previsao);
        form.setValorEstimado(new BigDecimal("1600.00"));
        service.atualizar(9L, form);

        assertThat(previsao.getValorEstimado()).isEqualByComparingTo("1600.00");
        assertThat(previsao.getValorConfirmado()).isEqualByComparingTo("1480.00");
        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.LIBERADO);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PrevisaoService.ENTIDADE), eq(9L),
                contains("valor estimado de R$ 1.500,00 para R$ 1.600,00"));
    }

    @Test
    void edicaoNaoMarcaComoLiberado() {
        PrevisaoBeneficio previsao = comId(previsao(HOJE, "1500.00"), 9L);
        when(previsaoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(previsao));
        PrevisaoForm form = PrevisaoForm.de(previsao);
        form.setSituacao(SituacaoPrevisao.LIBERADO);

        assertThatThrownBy(() -> service.atualizar(9L, form))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("situacao");
        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.PREVISTO);
    }

    @Test
    void edicaoSemMudancaNaoGeraAuditoria() {
        PrevisaoBeneficio previsao = comId(previsao(HOJE, "1500.00"), 9L);
        when(previsaoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(previsao));

        service.atualizar(9L, PrevisaoForm.de(previsao));

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void alterarSituacaoDePrevisaoLiberadaERejeitado() {
        PrevisaoBeneficio previsao = comId(previsao(HOJE, "1500.00"), 9L);
        previsao.confirmarLiberacao(HOJE, new BigDecimal("1500.00"));
        when(previsaoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(previsao));

        assertThatThrownBy(() -> service.alterarSituacao(9L, SituacaoPrevisao.ADIADO))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.LIBERADO);
    }

    @Test
    void desfazerLiberacaoVoltaParaAguardandoERegistraValoresAnteriores() {
        PrevisaoBeneficio previsao = comId(previsao(HOJE, "1500.00"), 9L);
        previsao.confirmarLiberacao(HOJE, new BigDecimal("1450.00"));
        when(previsaoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(previsao));

        service.desfazerLiberacao(9L);

        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.AGUARDANDO_CONFIRMACAO);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PrevisaoService.ENTIDADE), eq(9L),
                contains("R$ 1.450,00"));
    }

    private PrevisaoBeneficio previsao(LocalDate data, String valor) {
        return new PrevisaoBeneficio(atendimento, data, new BigDecimal(valor), TipoPagamento.PRIMEIRO_PAGAMENTO);
    }

    private static PrevisaoForm form(String valor, SituacaoPrevisao situacao) {
        PrevisaoForm form = new PrevisaoForm();
        form.setDataPrevista(HOJE.plusDays(15));
        form.setValorEstimado(new BigDecimal(valor));
        form.setTipoPagamento(TipoPagamento.PRIMEIRO_PAGAMENTO);
        form.setSituacao(situacao);
        return form;
    }

    private static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
