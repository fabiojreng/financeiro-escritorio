package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import br.com.escritorio.financeiro.domain.Despesa;
import br.com.escritorio.financeiro.domain.FechamentoDespesa;
import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import br.com.escritorio.financeiro.domain.SituacaoEnvioDespesas;
import br.com.escritorio.financeiro.dto.PendenciaEnvioDespesas;
import br.com.escritorio.financeiro.repository.DespesaRepository;
import br.com.escritorio.financeiro.repository.FechamentoDespesaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FechamentoDespesaServiceTest {

    private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);
    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);

    @Mock
    private FechamentoDespesaRepository fechamentoRepository;
    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @Test
    void marcarEnviadoGuardaQuantidadeETotalDaPlanilha() {
        FechamentoDespesaService service = servico(LocalDate.of(2026, 10, 16));
        when(fechamentoRepository.findByCompetencia(SETEMBRO.atDay(1))).thenReturn(Optional.empty());
        when(despesaRepository.ativasEntre(SETEMBRO.atDay(1), SETEMBRO.atEndOfMonth()))
                .thenReturn(List.of(despesa("100.00"), despesa("250.50")));
        when(auditoriaService.usuarioAtual()).thenReturn("ana");

        FechamentoDespesa fechamento = service.marcarEnviado(SETEMBRO);

        assertThat(fechamento.isEnviado()).isTrue();
        assertThat(fechamento.getCompetencia()).isEqualTo(SETEMBRO);
        assertThat(fechamento.getQuantidade()).isEqualTo(2);
        assertThat(fechamento.getTotal()).isEqualByComparingTo("350.50");
        assertThat(fechamento.getEnviadoPor()).isEqualTo("ana");
        verify(fechamentoRepository).save(fechamento);
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CONFIRMACAO), eq(FechamentoDespesaService.ENTIDADE), any(),
                descricao.capture());
        assertThat(descricao.getValue()).contains("setembro de 2026", "2 despesas", "R$ 350,50");
    }

    @Test
    void mesQueAindaNaoTerminouNaoPodeSerMarcadoComoEnviado() {
        FechamentoDespesaService service = servico(LocalDate.of(2026, 10, 31));

        assertThatThrownBy(() -> service.marcarEnviado(OUTUBRO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ainda não terminou");
        verify(fechamentoRepository, never()).save(any());
    }

    @Test
    void mesJaEnviadoNaoEMarcadoDeNovo() {
        FechamentoDespesaService service = servico(LocalDate.of(2026, 10, 16));
        when(fechamentoRepository.findByCompetencia(SETEMBRO.atDay(1))).thenReturn(Optional.of(enviado(SETEMBRO)));

        assertThatThrownBy(() -> service.marcarEnviado(SETEMBRO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já está marcada como enviada");
    }

    @Test
    void mesEnviadoTravaAlteracoes() {
        FechamentoDespesaService service = servico(LocalDate.of(2026, 10, 16));
        when(fechamentoRepository.findByCompetencia(SETEMBRO.atDay(1))).thenReturn(Optional.of(enviado(SETEMBRO)));

        assertThatThrownBy(() -> service.exigirAberto(SETEMBRO, "dataPagamento"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foram enviadas")
                .extracting("campo").isEqualTo("dataPagamento");
    }

    @Test
    void somenteQuemTemPermissaoReabreComMotivo() {
        FechamentoDespesaService service = servico(LocalDate.of(2026, 10, 16));
        FechamentoDespesa fechamento = enviado(SETEMBRO);

        assertThatThrownBy(() -> service.reabrir(SETEMBRO, "Faltou uma nota", false))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("permissão");
        assertThatThrownBy(() -> service.reabrir(SETEMBRO, " ", true))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("motivo");
        assertThat(fechamento.isEnviado()).isTrue();

        when(fechamentoRepository.findByCompetencia(SETEMBRO.atDay(1))).thenReturn(Optional.of(fechamento));
        when(auditoriaService.usuarioAtual()).thenReturn("admin");
        service.reabrir(SETEMBRO, "Faltou uma nota", true);

        assertThat(fechamento.isEnviado()).isFalse();
        assertThat(fechamento.getMotivoReabertura()).isEqualTo("Faltou uma nota");
        assertThat(fechamento.getReabertoPor()).isEqualTo("admin");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(FechamentoDespesaService.ENTIDADE), any(),
                anyString());
    }

    @Test
    void painelAvisaDoMesAnteriorAPartirDoDia15() {
        when(fechamentoRepository.competenciasEnviadas()).thenReturn(List.of());
        when(despesaRepository.datasAtivasAntesDe(SETEMBRO.atDay(1))).thenReturn(List.of());

        assertThat(servico(LocalDate.of(2026, 10, 14)).pendencias()).isEmpty();

        List<PendenciaEnvioDespesas> pendencias = servico(LocalDate.of(2026, 10, 15)).pendencias();
        assertThat(pendencias).singleElement().satisfies(p -> {
            assertThat(p.competencia()).isEqualTo(SETEMBRO);
            assertThat(p.nomeMes()).isEqualTo("setembro de 2026");
            assertThat(p.prazo()).isEqualTo(LocalDate.of(2026, 10, 20));
            assertThat(p.situacao()).isEqualTo(SituacaoEnvioDespesas.PRAZO_PROXIMO);
        });

        assertThat(servico(LocalDate.of(2026, 10, 21)).pendencias())
                .singleElement().extracting(PendenciaEnvioDespesas::situacao).isEqualTo(SituacaoEnvioDespesas.ATRASADO);
    }

    @Test
    void painelAvisaDeMesesAntigosComDespesasNaoEnviadas() {
        when(fechamentoRepository.competenciasEnviadas()).thenReturn(List.of(SETEMBRO.atDay(1),
                YearMonth.of(2026, 7).atDay(1)));
        when(despesaRepository.datasAtivasAntesDe(SETEMBRO.atDay(1))).thenReturn(List.of(
                LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 20), LocalDate.of(2026, 7, 10)));

        List<PendenciaEnvioDespesas> pendencias = servico(LocalDate.of(2026, 10, 2)).pendencias();

        assertThat(pendencias).singleElement().satisfies(p -> {
            assertThat(p.competencia()).isEqualTo(YearMonth.of(2026, 8));
            assertThat(p.situacao()).isEqualTo(SituacaoEnvioDespesas.ATRASADO);
        });
    }

    private FechamentoDespesaService servico(LocalDate hoje) {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        return new FechamentoDespesaService(fechamentoRepository, despesaRepository, auditoriaService,
                Clock.fixed(hoje.atStartOfDay(fuso).toInstant(), fuso));
    }

    private static FechamentoDespesa enviado(YearMonth competencia) {
        FechamentoDespesa fechamento = new FechamentoDespesa(competencia);
        fechamento.marcarEnviado("ana", LocalDateTime.of(2026, 10, 10, 9, 0), 1, new BigDecimal("10.00"));
        return fechamento;
    }

    private static Despesa despesa(String valor) {
        return new Despesa(SETEMBRO.atDay(5), new CategoriaDespesa("Energia Eletrica", null),
                new FormaPagamentoDespesa("BB", null), "Despesa c/ Energia", new BigDecimal(valor), "Companhia");
    }
}
