package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.AcertoMensal;
import br.com.escritorio.financeiro.domain.FechamentoFuncionario;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import br.com.escritorio.financeiro.domain.TipoLancamentoFuncionario;
import br.com.escritorio.financeiro.dto.AcertoFuncionario;
import br.com.escritorio.financeiro.dto.ResumoAcertosMes;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FechamentoFuncionarioServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 22);
    private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);
    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);
    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);

    @Mock
    private FechamentoFuncionarioRepository fechamentoRepository;
    @Mock
    private LancamentoFuncionarioRepository lancamentoRepository;
    @Mock
    private FuncionarioService funcionarioService;
    @Mock
    private AuditoriaService auditoriaService;

    @Test
    void marcarPagoGuardaORetratoDoAcerto() {
        Funcionario maria = maria();
        when(funcionarioService.buscar(1L)).thenReturn(maria);
        when(fechamentoRepository.findByFuncionarioIdAndCompetencia(1L, SETEMBRO.atDay(1))).thenReturn(Optional.empty());
        when(lancamentoRepository.daCompetencia(1L, SETEMBRO.atDay(1))).thenReturn(exemploDaDemanda(maria, SETEMBRO));
        when(auditoriaService.usuarioAtual()).thenReturn("chefe");

        FechamentoFuncionario fechamento = servico().marcarPago(1L, SETEMBRO, LocalDate.of(2026, 10, 5),
                FormaPagamento.PIX);

        assertThat(fechamento.isPago()).isTrue();
        assertThat(fechamento.getPagoPor()).isEqualTo("chefe");
        assertThat(fechamento.getAcerto().salarioBase()).isEqualByComparingTo("2500.00");
        assertThat(fechamento.getAcerto().vales()).isEqualByComparingTo("900.00");
        assertThat(fechamento.getLiquido()).isEqualByComparingTo("1830.00");
        verify(fechamentoRepository).save(fechamento);
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CONFIRMACAO), eq(FechamentoFuncionarioService.ENTIDADE),
                any(), descricao.capture());
        assertThat(descricao.getValue()).contains("Maria", "setembro de 2026", "R$ 1.830,00", "PIX");
    }

    @Test
    void acertoDeMesQueNaoComecouNaoPodeSerPago() {
        when(funcionarioService.buscar(1L)).thenReturn(maria());

        assertThatThrownBy(() -> servico().marcarPago(1L, NOVEMBRO, HOJE, FormaPagamento.PIX))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não começou");
    }

    @Test
    void dataDePagamentoNoFuturoEhRecusada() {
        when(funcionarioService.buscar(1L)).thenReturn(maria());

        assertThatThrownBy(() -> servico().marcarPago(1L, OUTUBRO, HOJE.plusDays(1), FormaPagamento.PIX))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("dataPagamento");
    }

    @Test
    void acertoJaPagoNaoEhPagoDeNovo() {
        Funcionario maria = maria();
        when(funcionarioService.buscar(1L)).thenReturn(maria);
        when(fechamentoRepository.findByFuncionarioIdAndCompetencia(1L, SETEMBRO.atDay(1)))
                .thenReturn(Optional.of(pago(maria, SETEMBRO)));

        assertThatThrownBy(() -> servico().marcarPago(1L, SETEMBRO, HOJE, FormaPagamento.PIX))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já está pago");
        verify(fechamentoRepository, never()).save(any());
    }

    @Test
    void acertoPagoMostraORetratoMesmoQueOSalarioMudeDepois() {
        Funcionario maria = maria();
        when(fechamentoRepository.findByFuncionarioIdAndCompetencia(1L, SETEMBRO.atDay(1)))
                .thenReturn(Optional.of(pago(maria, SETEMBRO)));
        maria.setSalarioBase(new BigDecimal("3000.00"));

        AcertoFuncionario acerto = servico().acerto(maria, SETEMBRO);

        assertThat(acerto.isPago()).isTrue();
        assertThat(acerto.acerto().salarioBase()).isEqualByComparingTo("2500.00");
        assertThat(acerto.acerto().liquido()).isEqualByComparingTo("1830.00");
    }

    @Test
    void competenciaPagaFicaFechadaParaLancamentos() {
        Funcionario maria = maria();
        when(fechamentoRepository.findByFuncionarioIdAndCompetencia(1L, SETEMBRO.atDay(1)))
                .thenReturn(Optional.of(pago(maria, SETEMBRO)));

        assertThatThrownBy(() -> servico().exigirAberto(maria, SETEMBRO, "competencia"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foi pago")
                .extracting("campo").isEqualTo("competencia");
    }

    @Test
    void reabrirExigePermissaoEMotivo() {
        assertThatThrownBy(() -> servico().reabrir(1L, SETEMBRO, "Faltou lançar a hora extra.", false))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("permissão");
        assertThatThrownBy(() -> servico().reabrir(1L, SETEMBRO, "erro", true))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("motivo");
    }

    @Test
    void reabrirVoltaOAcertoParaEmAbertoEAuditaOMotivo() {
        Funcionario maria = maria();
        FechamentoFuncionario fechamento = pago(maria, SETEMBRO);
        when(funcionarioService.buscar(1L)).thenReturn(maria);
        when(fechamentoRepository.findByFuncionarioIdAndCompetencia(1L, SETEMBRO.atDay(1)))
                .thenReturn(Optional.of(fechamento));
        when(auditoriaService.usuarioAtual()).thenReturn("chefe");

        servico().reabrir(1L, SETEMBRO, "Faltou lançar a hora extra.", true);

        assertThat(fechamento.isPago()).isFalse();
        assertThat(fechamento.getMotivoReabertura()).isEqualTo("Faltou lançar a hora extra.");
        assertThat(fechamento.getReabertoPor()).isEqualTo("chefe");
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(FechamentoFuncionarioService.ENTIDADE),
                any(), descricao.capture());
        assertThat(descricao.getValue()).contains("reaberto", "Faltou lançar a hora extra.");
    }

    @Test
    void resumoDoMesSoTrazQuemTrabalhouNoMes() {
        Funcionario maria = maria();
        Funcionario joao = new Funcionario("João", LocalDate.of(2026, 11, 3), new BigDecimal("1800.00"), 10);
        ReflectionTestUtils.setField(joao, "id", 2L);
        when(lancamentoRepository.ativosEntreCompetencias(OUTUBRO.atDay(1), OUTUBRO.atDay(1)))
                .thenReturn(exemploDaDemanda(maria, OUTUBRO));
        when(fechamentoRepository.entreCompetencias(OUTUBRO.atDay(1), OUTUBRO.atDay(1))).thenReturn(List.of());
        when(funcionarioService.listarPorNome()).thenReturn(List.of(joao, maria));

        ResumoAcertosMes resumo = servico().resumoDoMes(OUTUBRO);

        assertThat(resumo.acertos()).extracting(a -> a.funcionario().getNome()).containsExactly("Maria");
        assertThat(resumo.getTotalPendente()).isEqualByComparingTo("1830.00");
        assertThat(resumo.getTotalPago()).isEqualByComparingTo("0");
        assertThat(resumo.getCusto()).as("salário + extras + reembolsos − descontos")
                .isEqualByComparingTo("2730.00");
    }

    @Test
    void periodoDoRelatorioTemLimiteDeMeses() {
        assertThatThrownBy(() -> servico().resumoEntre(YearMonth.of(2023, 1), YearMonth.of(2026, 1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("36 meses");
    }

    private FechamentoFuncionarioService servico() {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        Clock relogio = Clock.fixed(HOJE.atStartOfDay(fuso).toInstant(), fuso);
        return new FechamentoFuncionarioService(fechamentoRepository, lancamentoRepository, funcionarioService,
                auditoriaService, relogio);
    }

    private static FechamentoFuncionario pago(Funcionario funcionario, YearMonth competencia) {
        FechamentoFuncionario fechamento = new FechamentoFuncionario(funcionario, competencia);
        fechamento.marcarPago(new AcertoMensal(new BigDecimal("2500.00"), new BigDecimal("200.00"),
                        new BigDecimal("80.00"), new BigDecimal("900.00"), new BigDecimal("50.00"),
                        new BigDecimal("1830.00")),
                LocalDate.of(2026, 10, 5), FormaPagamento.PIX, "chefe", HOJE.atStartOfDay());
        ReflectionTestUtils.setField(fechamento, "id", 30L);
        return fechamento;
    }

    private static List<LancamentoFuncionario> exemploDaDemanda(Funcionario funcionario, YearMonth competencia) {
        LancamentoFuncionario desconto = lancamento(funcionario, competencia, TipoLancamentoFuncionario.DESCONTO, "50.00");
        desconto.setDescricao("Falta não justificada");
        return List.of(
                lancamento(funcionario, competencia, TipoLancamentoFuncionario.VALE, "300.00"),
                lancamento(funcionario, competencia, TipoLancamentoFuncionario.VALE, "600.00"),
                lancamento(funcionario, competencia, TipoLancamentoFuncionario.BONIFICACAO, "200.00"),
                lancamento(funcionario, competencia, TipoLancamentoFuncionario.REEMBOLSO, "80.00"),
                desconto);
    }

    private static LancamentoFuncionario lancamento(Funcionario funcionario, YearMonth competencia,
                                                    TipoLancamentoFuncionario tipo, String valor) {
        return new LancamentoFuncionario(funcionario, tipo, competencia.atDay(5), competencia, new BigDecimal(valor));
    }

    private static Funcionario maria() {
        Funcionario maria = new Funcionario("Maria", LocalDate.of(2025, 3, 10), new BigDecimal("2500.00"), 5);
        ReflectionTestUtils.setField(maria, "id", 1L);
        return maria;
    }
}
