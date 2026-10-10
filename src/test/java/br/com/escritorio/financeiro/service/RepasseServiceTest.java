package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import br.com.escritorio.financeiro.domain.SituacaoRepasse;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.PagamentoRepasseForm;
import br.com.escritorio.financeiro.dto.RepasseForm;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.RepasseRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
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
class RepasseServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 10);

    @Mock
    private RepasseRepository repasseRepository;
    @Mock
    private AtendimentoRepository atendimentoRepository;
    @Mock
    private ResponsavelRepository responsavelRepository;
    @Mock
    private AuditoriaService auditoriaService;

    private RepasseService service;
    private Atendimento atendimento;
    private Responsavel ana;

    @BeforeEach
    void setUp() {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        service = new RepasseService(repasseRepository, atendimentoRepository, responsavelRepository, auditoriaService,
                Clock.fixed(HOJE.atStartOfDay(fuso).toInstant(), fuso));
        ana = comId(new Responsavel("Ana", null), 5L);
        atendimento = comId(new Atendimento(new Cliente("Maria", HOJE), new TipoServico("Aposentadoria", null),
                ana, HOJE), 3L);
    }

    @Test
    void lancamentoGuardaExatamenteOsValoresInformados() {
        when(atendimentoRepository.findComDetalhesById(3L)).thenReturn(Optional.of(atendimento));
        when(responsavelRepository.findById(5L)).thenReturn(Optional.of(ana));

        Repasse repasse = service.criar(3L, repasseForm("300.00"));

        assertThat(repasse.getValorPrevisto()).isEqualByComparingTo("300.00");
        assertThat(repasse.getValorPago()).isNull();
        assertThat(repasse.getSituacao()).isEqualTo(SituacaoRepasse.PENDENTE);
        assertThat(repasse.getDestinatario()).isSameAs(ana);
        verify(repasseRepository).save(repasse);
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(RepasseService.ENTIDADE), any(),
                descricao.capture());
        assertThat(descricao.getValue()).contains("R$ 300,00", "Ana", "Maria", "Comissão pela indicação");
    }

    @Test
    void atendimentoCanceladoNaoRecebeRepasse() {
        atendimento.setSituacao(SituacaoAtendimento.CANCELADO);
        when(atendimentoRepository.findComDetalhesById(3L)).thenReturn(Optional.of(atendimento));

        assertThatThrownBy(() -> service.criar(3L, repasseForm("300.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("cancelado");
        verify(repasseRepository, never()).save(any());
    }

    @Test
    void destinatarioInativoNaoRecebeNovoRepasse() {
        ana.setAtivo(false);
        when(atendimentoRepository.findComDetalhesById(3L)).thenReturn(Optional.of(atendimento));
        when(responsavelRepository.findById(5L)).thenReturn(Optional.of(ana));

        assertThatThrownBy(() -> service.criar(3L, repasseForm("300.00")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("destinatarioId");
    }

    @Test
    void pagamentoAceitaValorDiferenteDoPrevisto() {
        Repasse repasse = repasseExistente("300.00");

        service.registrarPagamento(20L, pagamentoForm(HOJE, "280.00"));

        assertThat(repasse.getSituacao()).isEqualTo(SituacaoRepasse.PAGO);
        assertThat(repasse.getValorPago()).isEqualByComparingTo("280.00");
        assertThat(repasse.getValorPrevisto()).isEqualByComparingTo("300.00");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CONFIRMACAO), eq(RepasseService.ENTIDADE), eq(20L),
                anyString());
    }

    @Test
    void pagamentoComDataFuturaERecusado() {
        Repasse repasse = repasseExistente("300.00");

        assertThatThrownBy(() -> service.registrarPagamento(20L, pagamentoForm(HOJE.plusDays(1), "300.00")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("dataPagamento");
        assertThat(repasse.isPendente()).isTrue();
    }

    @Test
    void repassePagoNaoPodeSerEditado() {
        Repasse repasse = repasseExistente("300.00");
        repasse.registrarPagamento(HOJE, new BigDecimal("300.00"));

        assertThatThrownBy(() -> service.atualizar(20L, repasseForm("350.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("pendentes");
        assertThat(repasse.getValorPrevisto()).isEqualByComparingTo("300.00");
    }

    @Test
    void cancelamentoExigeMotivo() {
        Repasse repasse = repasseExistente("300.00");

        assertThatThrownBy(() -> service.cancelar(20L, "  ", false))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("motivo");
        assertThat(repasse.isPendente()).isTrue();
    }

    @Test
    void operadorCancelaRepassePendente() {
        Repasse repasse = repasseExistente("300.00");
        when(auditoriaService.usuarioAtual()).thenReturn("ana");

        service.cancelar(20L, "Comissão não combinada", false);

        assertThat(repasse.isCancelado()).isTrue();
        assertThat(repasse.getCanceladoPor()).isEqualTo("ana");
        verify(repasseRepository, never()).delete(any());
    }

    @Test
    void somenteAdministradorCancelaRepassePago() {
        Repasse repasse = repasseExistente("300.00");
        repasse.registrarPagamento(HOJE, new BigDecimal("300.00"));

        assertThatThrownBy(() -> service.cancelar(20L, "Pago em duplicidade", false))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("administrador");
        assertThat(repasse.isPago()).isTrue();

        when(auditoriaService.usuarioAtual()).thenReturn("admin");
        service.cancelar(20L, "Pago em duplicidade", true);

        assertThat(repasse.isCancelado()).isTrue();
        assertThat(repasse.getValorPago()).isEqualByComparingTo("300.00");
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CANCELAMENTO), eq(RepasseService.ENTIDADE), eq(20L),
                descricao.capture());
        assertThat(descricao.getValue()).contains("pago", "R$ 300,00", "Pago em duplicidade");
    }

    private Repasse repasseExistente(String valor) {
        Repasse repasse = comId(new Repasse(atendimento, ana, "Comissão pela indicação", new BigDecimal(valor), HOJE), 20L);
        when(repasseRepository.findComDetalhesById(20L)).thenReturn(Optional.of(repasse));
        return repasse;
    }

    private static RepasseForm repasseForm(String valor) {
        RepasseForm form = new RepasseForm();
        form.setDestinatarioId(5L);
        form.setDescricao("Comissão pela indicação");
        form.setValorPrevisto(new BigDecimal(valor));
        form.setDataPrevista(HOJE);
        return form;
    }

    private static PagamentoRepasseForm pagamentoForm(LocalDate data, String valor) {
        PagamentoRepasseForm form = new PagamentoRepasseForm();
        form.setDataPagamento(data);
        form.setValorPago(new BigDecimal(valor));
        return form;
    }

    private static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
