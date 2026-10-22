package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import br.com.escritorio.financeiro.domain.SituacaoLancamentoFuncionario;
import br.com.escritorio.financeiro.domain.TipoLancamentoFuncionario;
import br.com.escritorio.financeiro.dto.LancamentoFuncionarioForm;
import br.com.escritorio.financeiro.repository.LancamentoFuncionarioRepository;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LancamentoFuncionarioServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 22);
    private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);
    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);

    @Mock
    private LancamentoFuncionarioRepository lancamentoRepository;
    @Mock
    private FuncionarioService funcionarioService;
    @Mock
    private FechamentoFuncionarioService fechamentoService;
    @Mock
    private AuditoriaService auditoriaService;

    @Test
    void valeEntraNaCompetenciaDaDataQuandoElaNaoEhInformada() {
        Funcionario maria = maria();
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria);

        LancamentoFuncionario vale = servico().criar(1L, form(TipoLancamentoFuncionario.VALE, "300.00"));

        assertThat(vale.getCompetencia()).isEqualTo(OUTUBRO);
        assertThat(vale.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(vale.getSituacao()).isEqualTo(SituacaoLancamentoFuncionario.ATIVO);
        verify(fechamentoService).exigirAberto(maria, OUTUBRO, "competencia");
        verify(lancamentoRepository).save(vale);
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(LancamentoFuncionarioService.ENTIDADE),
                any(), descricao.capture());
        assertThat(descricao.getValue()).contains("Vale", "R$ 300,00", "Maria", "outubro de 2026");
    }

    @Test
    void lancamentoPodeIrParaOutraCompetencia() {
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria());
        LancamentoFuncionarioForm form = form(TipoLancamentoFuncionario.HORA_EXTRA, "150.00");
        form.setCompetencia(SETEMBRO);

        LancamentoFuncionario horaExtra = servico().criar(1L, form);

        assertThat(horaExtra.getCompetencia()).isEqualTo(SETEMBRO);
        assertThat(horaExtra.getFormaPagamento()).as("só vale e reembolso são pagos na hora").isNull();
    }

    @Test
    void descontoExigeMotivo() {
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria());
        LancamentoFuncionarioForm form = form(TipoLancamentoFuncionario.DESCONTO, "50.00");
        form.setDescricao("   ");

        assertThatThrownBy(() -> servico().criar(1L, form))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("descricao");
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void valeExigeFormaDePagamento() {
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria());
        LancamentoFuncionarioForm form = form(TipoLancamentoFuncionario.VALE, "300.00");
        form.setFormaPagamento(null);

        assertThatThrownBy(() -> servico().criar(1L, form))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("formaPagamento");
    }

    @Test
    void dataNoFuturoEhRecusada() {
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria());
        LancamentoFuncionarioForm form = form(TipoLancamentoFuncionario.VALE, "300.00");
        form.setData(HOJE.plusDays(1));

        assertThatThrownBy(() -> servico().criar(1L, form))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("data");
    }

    @Test
    void parcelaDeEmprestimoNaoEhLancadaAMao() {
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria());

        assertThatThrownBy(() -> servico().criar(1L, form(TipoLancamentoFuncionario.PARCELA_EMPRESTIMO, "100.00")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("tipo");
    }

    @Test
    void competenciaForaDoPeriodoTrabalhadoEhRecusada() {
        Funcionario maria = maria();
        maria.setDataDesligamento(LocalDate.of(2026, 8, 31));
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria);
        LancamentoFuncionarioForm form = form(TipoLancamentoFuncionario.BONIFICACAO, "200.00");
        form.setCompetencia(SETEMBRO);

        assertThatThrownBy(() -> servico().criar(1L, form))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não trabalhava")
                .extracting("campo").isEqualTo("competencia");
    }

    @Test
    void competenciaComAcertoPagoNaoRecebeLancamentos() {
        Funcionario maria = maria();
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria);
        doThrow(new RegraNegocioException("competencia", "O acerto já foi pago."))
                .when(fechamentoService).exigirAberto(maria, OUTUBRO, "competencia");

        assertThatThrownBy(() -> servico().criar(1L, form(TipoLancamentoFuncionario.VALE, "300.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foi pago");
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void alterarParaUmaCompetenciaPagaEhBloqueado() {
        Funcionario maria = maria();
        LancamentoFuncionario vale = vale(maria);
        when(lancamentoRepository.findComDetalhesById(10L)).thenReturn(Optional.of(vale));
        lenient().doThrow(new RegraNegocioException("competencia", "O acerto já foi pago."))
                .when(fechamentoService).exigirAberto(maria, SETEMBRO, "competencia");
        LancamentoFuncionarioForm form = LancamentoFuncionarioForm.de(vale);
        form.setCompetencia(SETEMBRO);

        assertThatThrownBy(() -> servico().atualizar(10L, form)).isInstanceOf(RegraNegocioException.class);
        assertThat(vale.getCompetencia()).isEqualTo(OUTUBRO);
    }

    @Test
    void alterarAuditaValorAntigoENovo() {
        LancamentoFuncionario vale = vale(maria());
        when(lancamentoRepository.findComDetalhesById(10L)).thenReturn(Optional.of(vale));
        LancamentoFuncionarioForm form = LancamentoFuncionarioForm.de(vale);
        form.setValor(new BigDecimal("350.00"));

        servico().atualizar(10L, form);

        assertThat(vale.getValor()).isEqualByComparingTo("350.00");
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(LancamentoFuncionarioService.ENTIDADE),
                eq(10L), descricao.capture());
        assertThat(descricao.getValue()).contains("valor de R$ 300,00 para R$ 350,00");
    }

    @Test
    void cancelarExigeMotivoENaoApagaOLancamento() {
        LancamentoFuncionario vale = vale(maria());
        when(lancamentoRepository.findComDetalhesById(10L)).thenReturn(Optional.of(vale));

        assertThatThrownBy(() -> servico().cancelar(10L, " ")).isInstanceOf(RegraNegocioException.class);
        assertThat(vale.isAtivo()).isTrue();

        when(auditoriaService.usuarioAtual()).thenReturn("chefe");
        servico().cancelar(10L, "Lançado em duplicidade.");

        assertThat(vale.isCancelado()).isTrue();
        assertThat(vale.getMotivoCancelamento()).isEqualTo("Lançado em duplicidade.");
        assertThat(vale.getCanceladoPor()).isEqualTo("chefe");
        verify(lancamentoRepository, never()).delete(any());
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CANCELAMENTO), eq(LancamentoFuncionarioService.ENTIDADE),
                eq(10L), descricao.capture());
        assertThat(descricao.getValue()).contains("Lançado em duplicidade.");
    }

    @Test
    void lancamentoCanceladoNaoPodeSerAlterado() {
        LancamentoFuncionario vale = vale(maria());
        vale.cancelar("Lançado em duplicidade.", "chefe", HOJE.atStartOfDay());
        when(lancamentoRepository.findComDetalhesById(10L)).thenReturn(Optional.of(vale));

        assertThatThrownBy(() -> servico().atualizar(10L, LancamentoFuncionarioForm.de(vale)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("cancelado");
    }

    private LancamentoFuncionarioService servico() {
        Clock relogio = Clock.fixed(HOJE.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant(),
                ZoneId.of("America/Sao_Paulo"));
        return new LancamentoFuncionarioService(lancamentoRepository, funcionarioService, fechamentoService,
                auditoriaService, relogio);
    }

    private static LancamentoFuncionarioForm form(TipoLancamentoFuncionario tipo, String valor) {
        LancamentoFuncionarioForm form = new LancamentoFuncionarioForm();
        form.setTipo(tipo);
        form.setData(LocalDate.of(2026, 10, 5));
        form.setValor(new BigDecimal(valor));
        form.setFormaPagamento(FormaPagamento.PIX);
        if (tipo.isExigeMotivo()) {
            form.setDescricao("Falta não justificada");
        }
        return form;
    }

    private static LancamentoFuncionario vale(Funcionario funcionario) {
        LancamentoFuncionario vale = new LancamentoFuncionario(funcionario, TipoLancamentoFuncionario.VALE,
                LocalDate.of(2026, 10, 5), OUTUBRO, new BigDecimal("300.00"));
        vale.setFormaPagamento(FormaPagamento.PIX);
        ReflectionTestUtils.setField(vale, "id", 10L);
        return vale;
    }

    private static Funcionario maria() {
        Funcionario maria = new Funcionario("Maria", LocalDate.of(2025, 3, 10), new BigDecimal("2500.00"), 5);
        ReflectionTestUtils.setField(maria, "id", 1L);
        return maria;
    }
}
