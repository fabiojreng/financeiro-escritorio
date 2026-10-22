package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.EmprestimoFuncionario;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import br.com.escritorio.financeiro.domain.SituacaoEmprestimo;
import br.com.escritorio.financeiro.domain.TipoLancamentoFuncionario;
import br.com.escritorio.financeiro.dto.EmprestimoFuncionarioForm;
import br.com.escritorio.financeiro.dto.SaldoEmprestimo;
import br.com.escritorio.financeiro.repository.EmprestimoFuncionarioRepository;
import br.com.escritorio.financeiro.repository.FechamentoFuncionarioRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmprestimoFuncionarioServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 22);
    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);
    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);
    private static final YearMonth DEZEMBRO = YearMonth.of(2026, 12);
    private static final YearMonth JANEIRO = YearMonth.of(2027, 1);

    @Mock
    private EmprestimoFuncionarioRepository emprestimoRepository;
    @Mock
    private LancamentoFuncionarioRepository lancamentoRepository;
    @Mock
    private FechamentoFuncionarioRepository fechamentoRepository;
    @Mock
    private FuncionarioService funcionarioService;
    @Mock
    private FechamentoFuncionarioService fechamentoService;
    @Mock
    private AuditoriaService auditoriaService;

    @Test
    void criarGeraAsParcelasComOsCentavosQueSobramNaUltima() {
        Funcionario maria = maria();
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria);
        when(fechamentoService.competenciasPagas(1L)).thenReturn(Set.of());

        EmprestimoFuncionario emprestimo = servico().criar(1L, form("1000.00", 3, NOVEMBRO));

        assertThat(emprestimo.getSituacao()).isEqualTo(SituacaoEmprestimo.EM_ABERTO);
        assertThat(emprestimo.getUltimaCompetencia()).isEqualTo(JANEIRO);
        verify(emprestimoRepository).save(emprestimo);
        ArgumentCaptor<LancamentoFuncionario> parcelas = ArgumentCaptor.forClass(LancamentoFuncionario.class);
        verify(lancamentoRepository, times(3)).save(parcelas.capture());
        assertThat(parcelas.getAllValues())
                .extracting(LancamentoFuncionario::getTipo, LancamentoFuncionario::getNumeroParcela,
                        LancamentoFuncionario::getCompetencia, l -> l.getValor().toPlainString(),
                        LancamentoFuncionario::getDescricao)
                .containsExactly(
                        tuple(TipoLancamentoFuncionario.PARCELA_EMPRESTIMO, 1, NOVEMBRO, "333.33",
                                "Parcela 1/3 do empréstimo"),
                        tuple(TipoLancamentoFuncionario.PARCELA_EMPRESTIMO, 2, DEZEMBRO, "333.33",
                                "Parcela 2/3 do empréstimo"),
                        tuple(TipoLancamentoFuncionario.PARCELA_EMPRESTIMO, 3, JANEIRO, "333.34",
                                "Parcela 3/3 do empréstimo"));
        assertThat(parcelas.getAllValues()).allMatch(p -> p.getEmprestimo() == emprestimo);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(EmprestimoFuncionarioService.ENTIDADE),
                any(), any());
    }

    @Test
    void primeiraParcelaNaoPodeSerAntesDoMesDoEmprestimo() {
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria());

        assertThatThrownBy(() -> servico().criar(1L, form("1000.00", 2, OUTUBRO.minusMonths(1))))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("primeiraCompetencia");
        verify(emprestimoRepository, never()).save(any());
    }

    @Test
    void parcelasDepoisDoDesligamentoSaoRecusadas() {
        Funcionario maria = maria();
        maria.setDataDesligamento(LocalDate.of(2026, 12, 15));
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria);

        assertThatThrownBy(() -> servico().criar(1L, form("1000.00", 3, NOVEMBRO)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("janeiro de 2027")
                .extracting("campo").isEqualTo("numeroParcelas");
    }

    @Test
    void parcelaNaoPodeCairEmCompetenciaComAcertoPago() {
        Funcionario maria = maria();
        when(funcionarioService.buscarAtivo(1L)).thenReturn(maria);
        when(fechamentoService.competenciasPagas(1L)).thenReturn(Set.of(OUTUBRO));
        doThrow(new RegraNegocioException("primeiraCompetencia", "O acerto de outubro já foi pago."))
                .when(fechamentoService).exigirAberto(maria, OUTUBRO, "primeiraCompetencia");

        assertThatThrownBy(() -> servico().criar(1L, form("600.00", 2, OUTUBRO)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foi pago");
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void saldoDevedorSaoAsParcelasDeCompetenciasAindaNaoPagas() {
        EmprestimoFuncionario emprestimo = emprestimo(maria());
        when(fechamentoService.competenciasPagas(1L)).thenReturn(Set.of(NOVEMBRO));
        when(lancamentoRepository.doEmprestimo(20L)).thenReturn(parcelas(emprestimo));

        SaldoEmprestimo saldo = servico().saldo(emprestimo);

        assertThat(saldo.saldo()).isEqualByComparingTo("666.67");
        assertThat(saldo.parcelasRestantes()).isEqualTo(2);
        assertThat(saldo.isTotalmenteDescontado()).isFalse();
    }

    @Test
    void quitacaoAntecipadaJuntaAsParcelasRestantesNumaSo() {
        EmprestimoFuncionario emprestimo = emprestimo(maria());
        List<LancamentoFuncionario> parcelas = parcelas(emprestimo);
        when(emprestimoRepository.findComFuncionarioById(20L)).thenReturn(Optional.of(emprestimo));
        when(fechamentoService.competenciasPagas(1L)).thenReturn(Set.of(NOVEMBRO));
        when(lancamentoRepository.doEmprestimo(20L)).thenReturn(parcelas);
        when(auditoriaService.usuarioAtual()).thenReturn("chefe");

        servico().quitar(20L, DEZEMBRO);

        assertThat(emprestimo.getSituacao()).isEqualTo(SituacaoEmprestimo.QUITADO);
        assertThat(parcelas.get(0).isAtivo()).as("parcela já descontada fica como está").isTrue();
        assertThat(parcelas.subList(1, 3)).allMatch(LancamentoFuncionario::isCancelado);
        ArgumentCaptor<LancamentoFuncionario> quitacao = ArgumentCaptor.forClass(LancamentoFuncionario.class);
        verify(lancamentoRepository).save(quitacao.capture());
        assertThat(quitacao.getValue().getValor()).isEqualByComparingTo("666.67");
        assertThat(quitacao.getValue().getCompetencia()).isEqualTo(DEZEMBRO);
        assertThat(quitacao.getValue().getNumeroParcela()).isNull();
        verify(fechamentoService).exigirAberto(emprestimo.getFuncionario(), DEZEMBRO, "competencia");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CONFIRMACAO), eq(EmprestimoFuncionarioService.ENTIDADE),
                eq(20L), any());
    }

    @Test
    void cancelamentoEhBloqueadoQuandoAlgumaParcelaJaFoiDescontada() {
        EmprestimoFuncionario emprestimo = emprestimo(maria());
        when(emprestimoRepository.findComFuncionarioById(20L)).thenReturn(Optional.of(emprestimo));
        when(fechamentoService.competenciasPagas(1L)).thenReturn(Set.of(NOVEMBRO));
        when(lancamentoRepository.doEmprestimo(20L)).thenReturn(parcelas(emprestimo));

        assertThatThrownBy(() -> servico().cancelar(20L, "Lançado por engano."))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("quitação antecipada");
        assertThat(emprestimo.getSituacao()).isEqualTo(SituacaoEmprestimo.EM_ABERTO);
    }

    @Test
    void cancelarCancelaTodasAsParcelas() {
        EmprestimoFuncionario emprestimo = emprestimo(maria());
        List<LancamentoFuncionario> parcelas = parcelas(emprestimo);
        when(emprestimoRepository.findComFuncionarioById(20L)).thenReturn(Optional.of(emprestimo));
        when(fechamentoService.competenciasPagas(1L)).thenReturn(Set.of());
        when(lancamentoRepository.doEmprestimo(20L)).thenReturn(parcelas);
        when(auditoriaService.usuarioAtual()).thenReturn("chefe");

        servico().cancelar(20L, "Lançado por engano.");

        assertThat(emprestimo.getSituacao()).isEqualTo(SituacaoEmprestimo.CANCELADO);
        assertThat(emprestimo.getMotivoCancelamento()).isEqualTo("Lançado por engano.");
        assertThat(parcelas).allMatch(LancamentoFuncionario::isCancelado);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CANCELAMENTO), eq(EmprestimoFuncionarioService.ENTIDADE),
                eq(20L), any());
    }

    @Test
    void emprestimoQuitadoNaoPodeSerCancelado() {
        EmprestimoFuncionario emprestimo = emprestimo(maria());
        emprestimo.quitar("chefe", HOJE.atStartOfDay());
        when(emprestimoRepository.findComFuncionarioById(20L)).thenReturn(Optional.of(emprestimo));

        assertThatThrownBy(() -> servico().cancelar(20L, "Lançado por engano."))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("quitado");
    }

    private EmprestimoFuncionarioService servico() {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        Clock relogio = Clock.fixed(HOJE.atStartOfDay(fuso).toInstant(), fuso);
        return new EmprestimoFuncionarioService(emprestimoRepository, lancamentoRepository, fechamentoRepository,
                funcionarioService, fechamentoService, auditoriaService, relogio);
    }

    private static EmprestimoFuncionarioForm form(String valor, int parcelas, YearMonth primeira) {
        EmprestimoFuncionarioForm form = new EmprestimoFuncionarioForm();
        form.setDataConcessao(LocalDate.of(2026, 10, 10));
        form.setValorTotal(new BigDecimal(valor));
        form.setNumeroParcelas(parcelas);
        form.setPrimeiraCompetencia(primeira);
        form.setFormaPagamento(FormaPagamento.PIX);
        return form;
    }

    private static EmprestimoFuncionario emprestimo(Funcionario funcionario) {
        EmprestimoFuncionario emprestimo = new EmprestimoFuncionario(funcionario, LocalDate.of(2026, 10, 10),
                new BigDecimal("1000.00"), 3, NOVEMBRO, FormaPagamento.PIX);
        ReflectionTestUtils.setField(emprestimo, "id", 20L);
        return emprestimo;
    }

    private static List<LancamentoFuncionario> parcelas(EmprestimoFuncionario emprestimo) {
        List<LancamentoFuncionario> parcelas = new ArrayList<>();
        String[] valores = {"333.33", "333.33", "333.34"};
        for (int i = 0; i < valores.length; i++) {
            parcelas.add(LancamentoFuncionario.parcela(emprestimo, i + 1, NOVEMBRO.plusMonths(i),
                    new BigDecimal(valores[i]), "Parcela " + (i + 1) + "/3 do empréstimo"));
        }
        return parcelas;
    }

    private static Funcionario maria() {
        Funcionario maria = new Funcionario("Maria", LocalDate.of(2025, 3, 10), new BigDecimal("2500.00"), 5);
        ReflectionTestUtils.setField(maria, "id", 1L);
        return maria;
    }
}
