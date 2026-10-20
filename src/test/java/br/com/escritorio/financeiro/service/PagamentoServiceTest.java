package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.ResponsavelPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.PagamentoForm;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.NegociacaoHonorarioRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelPagamentoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 14);
    private static final String CPF = "529.982.247-25";
    private static final String CPF_DIGITOS = "52998224725";

    @Mock
    private PagamentoAgendadoRepository pagamentoRepository;
    @Mock
    private ResponsavelRepository responsavelRepository;
    @Mock
    private TipoServicoRepository tipoServicoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private NegociacaoHonorarioRepository negociacaoHonorarioRepository;
    @Mock
    private ResponsavelPagamentoRepository responsavelPagamentoRepository;

    private PagamentoService service;
    private Responsavel responsavel;
    private Responsavel outroResponsavel;
    private TipoServico tipo;

    @BeforeEach
    void setUp() {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        Clock relogio = Clock.fixed(HOJE.atStartOfDay(fuso).toInstant(), fuso);
        ClienteService clienteService = new ClienteService(clienteRepository, auditoriaService);
        service = new PagamentoService(pagamentoRepository, responsavelRepository, responsavelPagamentoRepository,
                tipoServicoRepository, clienteService, auditoriaService, negociacaoHonorarioRepository, relogio);
        responsavel = comId(new Responsavel("Ana", null), 1L);
        outroResponsavel = comId(new Responsavel("Bruno", null), 3L);
        tipo = comId(new TipoServico("Salário-maternidade", null), 2L);
    }

    @Test
    void cpfNovoCadastraClienteComNomeECpfELancaPagamento() {
        quandoResponsavelETipoExistem();
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.empty());

        PagamentoAgendado pagamento = service.criar(form("  Maria da Silva ", CPF, "1500.00"));

        Cliente cliente = pagamento.getCliente();
        assertThat(cliente.getNome()).isEqualTo("Maria da Silva");
        assertThat(cliente.getCpf()).isEqualTo(CPF_DIGITOS);
        assertThat(cliente.getDataCadastro()).isEqualTo(HOJE);
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.AGENDADO);
        assertThat(pagamento.getBanco()).isEqualTo("Caixa Econômica Federal");
        verify(clienteRepository).save(cliente);
        verify(pagamentoRepository).save(pagamento);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(ClienteService.ENTIDADE), any(),
                contains("agenda de pagamentos"));
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(PagamentoService.ENTIDADE), any(),
                contains("R$ 1.500,00"));
    }

    @Test
    void cpfJaCadastradoReaproveitaClienteMesmoComAcentosEMaiusculasDiferentes() {
        quandoResponsavelETipoExistem();
        Cliente existente = comId(new Cliente("Maria José da Silva", HOJE.minusYears(1)), 7L);
        existente.setCpf(CPF_DIGITOS);
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.of(existente));

        PagamentoAgendado pagamento = service.criar(form("MARIA  JOSE DA SILVA", "52998224725", "800.00"));

        assertThat(pagamento.getCliente()).isSameAs(existente);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void cpfDeOutroClienteERecusadoMostrandoONomeCadastrado() {
        quandoResponsavelETipoExistem();
        Cliente existente = comId(new Cliente("Joana Pereira", HOJE), 7L);
        existente.setCpf(CPF_DIGITOS);
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.criar(form("Maria da Silva", CPF, "800.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Joana Pereira")
                .extracting("campo").isEqualTo("cpf");
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    void cpfInvalidoERecusado() {
        quandoResponsavelETipoExistem();

        assertThatThrownBy(() -> service.criar(form("Maria", "123.456.789-00", "800.00")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("cpf");
        verify(clienteRepository, never()).findByCpf(anyString());
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    void valorZeroERecusado() {
        assertThatThrownBy(() -> service.criar(form("Maria", CPF, "0.00")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("valor");
        verifyNoInteractions(clienteRepository, pagamentoRepository);
    }

    @Test
    void responsavelInativoNaoPodeSerUsadoEmNovoPagamento() {
        responsavel.setAtivo(false);
        when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavel));

        assertThatThrownBy(() -> service.criar(form("Maria", CPF, "800.00")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("responsavelIds");
    }

    @Test
    void pagamentoPodeTerVariosResponsaveisSemRepetir() {
        quandoResponsavelETipoExistem();
        when(responsavelRepository.findById(3L)).thenReturn(Optional.of(outroResponsavel));
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.empty());
        PagamentoForm form = form("Maria da Silva", CPF, "1500.00");
        form.setResponsavelIds(new ArrayList<>(List.of(3L, 1L, 3L)));

        PagamentoAgendado pagamento = service.criar(form);

        assertThat(pagamento.getResponsaveis()).extracting(r -> r.getResponsavel().getNome())
                .containsExactly("Ana", "Bruno");
        assertThat(pagamento.getNomesResponsaveis()).isEqualTo("Ana, Bruno");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(PagamentoService.ENTIDADE), any(),
                contains("responsáveis Ana, Bruno"));
    }

    @Test
    void semResponsavelOPagamentoERecusado() {
        PagamentoForm form = form("Maria", CPF, "800.00");
        form.setResponsavelIds(new ArrayList<>());

        assertThatThrownBy(() -> service.criar(form))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Selecione ao menos um responsável.")
                .extracting("campo").isEqualTo("responsavelIds");
        verifyNoInteractions(clienteRepository, pagamentoRepository);
    }

    @Test
    void edicaoIncluiERemoveResponsaveisERegistraNaAuditoria() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.of(pagamento.getCliente()));
        when(responsavelRepository.findById(3L)).thenReturn(Optional.of(outroResponsavel));
        PagamentoForm form = form("Maria da Silva", CPF, "1500.00");
        form.setResponsavelIds(new ArrayList<>(List.of(3L)));

        service.atualizar(9L, form);

        assertThat(pagamento.getNomesResponsaveis()).isEqualTo("Bruno");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PagamentoService.ENTIDADE), eq(9L),
                contains("responsáveis: incluído 'Bruno', removido 'Ana'"));
    }

    @Test
    void responsavelComRepassePagoNaoSaiDoPagamento() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        ResponsavelPagamento ana = pagamento.getResponsaveis().get(0);
        ana.setValorRepasse(new BigDecimal("100.00"));
        ana.setSituacaoRepasse(SituacaoRepassePagamento.PAGO);
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.of(pagamento.getCliente()));
        PagamentoForm form = form("Maria da Silva", CPF, "1500.00");
        form.setResponsavelIds(new ArrayList<>(List.of(3L)));

        assertThatThrownBy(() -> service.atualizar(9L, form))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("'Ana' já foi pago")
                .extracting("campo").isEqualTo("responsavelIds");
        assertThat(pagamento.getNomesResponsaveis()).isEqualTo("Ana");
    }

    @Test
    void repasseELancadoParaCadaResponsavelSeparadamente() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        ResponsavelPagamento bruno = comId(pagamento.adicionarResponsavel(outroResponsavel), 21L);
        when(responsavelPagamentoRepository.findComDetalhesById(21L)).thenReturn(Optional.of(bruno));

        service.lancarRepasse(21L, new BigDecimal("80.00"), SituacaoRepassePagamento.PAGO);

        assertThat(bruno.getValorRepasse()).isEqualByComparingTo("80.00");
        assertThat(bruno.getSituacaoRepasse()).isEqualTo(SituacaoRepassePagamento.PAGO);
        ResponsavelPagamento ana = pagamento.responsavel(1L).orElseThrow();
        assertThat(ana.getValorRepasse()).isNull();
        assertThat(ana.getSituacaoRepasse()).isEqualTo(SituacaoRepassePagamento.PENDENTE);
        assertThat(pagamento.getTotalRepasse()).isEqualByComparingTo("80.00");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PagamentoService.ENTIDADE), eq(9L),
                contains("repasse de 'Bruno' de sem valor para R$ 80,00"));
    }

    @Test
    void trocaDeStatusERegistradaNaAuditoria() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));

        service.alterarStatus(9L, StatusPagamento.PAGO);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PAGO);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PagamentoService.ENTIDADE), eq(9L),
                contains("status de Agendado para Pago"));
    }

    @Test
    void manterOMesmoStatusNaoGeraAuditoria() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));

        service.alterarStatus(9L, StatusPagamento.AGENDADO);

        verifyNoInteractions(auditoriaService);
    }

    @Test
    void edicaoRegistraOQueMudou() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.of(pagamento.getCliente()));
        PagamentoForm form = form("Maria da Silva", CPF, "1600.00");
        form.setBanco("Banco do Brasil");

        service.atualizar(9L, form);

        assertThat(pagamento.getValor()).isEqualByComparingTo("1600.00");
        assertThat(pagamento.getBanco()).isEqualTo("Banco do Brasil");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PagamentoService.ENTIDADE), eq(9L),
                contains("banco de Caixa Econômica Federal para Banco do Brasil"));
    }

    private void quandoResponsavelETipoExistem() {
        when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavel));
        when(tipoServicoRepository.findById(2L)).thenReturn(Optional.of(tipo));
    }

    private PagamentoAgendado pagamentoSalvo() {
        Cliente cliente = comId(new Cliente("Maria da Silva", HOJE), 7L);
        cliente.setCpf(CPF_DIGITOS);
        PagamentoAgendado pagamento = comId(new PagamentoAgendado(cliente, List.of(responsavel), tipo,
                HOJE.plusDays(2), new BigDecimal("1500.00"), "Caixa Econômica Federal"), 9L);
        comId(pagamento.getResponsaveis().get(0), 20L);
        return pagamento;
    }

    private PagamentoForm form(String nome, String cpf, String valor) {
        PagamentoForm form = new PagamentoForm();
        form.setNomeCliente(nome);
        form.setCpf(cpf);
        form.setResponsavelIds(new ArrayList<>(List.of(1L)));
        form.setTipoServicoId(2L);
        form.setDataRecebimento(HOJE.plusDays(2));
        form.setValor(new BigDecimal(valor));
        form.setBanco(" Caixa Econômica Federal ");
        form.setStatus(StatusPagamento.AGENDADO);
        return form;
    }

    private static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
