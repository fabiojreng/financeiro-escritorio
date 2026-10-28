package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.NegociacaoHonorario;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.ResponsavelPagamento;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoNegociacao;
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
import static org.mockito.Mockito.mock;
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

        PagamentoAgendado pagamento = service.criar(form("  Maria da Silva ", CPF, "1500.00"), false);

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

        PagamentoAgendado pagamento = service.criar(form("MARIA  JOSE DA SILVA", "52998224725", "800.00"), false);

        assertThat(pagamento.getCliente()).isSameAs(existente);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void cpfDeOutroClienteERecusadoMostrandoONomeCadastrado() {
        quandoResponsavelETipoExistem();
        Cliente existente = comId(new Cliente("Joana Pereira", HOJE), 7L);
        existente.setCpf(CPF_DIGITOS);
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.criar(form("Maria da Silva", CPF, "800.00"), false))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Joana Pereira")
                .extracting("campo").isEqualTo("cpf");
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    void cpfInvalidoERecusado() {
        quandoResponsavelETipoExistem();

        assertThatThrownBy(() -> service.criar(form("Maria", "123.456.789-00", "800.00"), false))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("cpf");
        verify(clienteRepository, never()).findByCpf(anyString());
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    void valorZeroERecusado() {
        assertThatThrownBy(() -> service.criar(form("Maria", CPF, "0.00"), false))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("valor");
        verifyNoInteractions(clienteRepository, pagamentoRepository);
    }

    @Test
    void responsavelInativoNaoPodeSerUsadoEmNovoPagamento() {
        responsavel.setAtivo(false);
        when(responsavelRepository.findById(1L)).thenReturn(Optional.of(responsavel));

        assertThatThrownBy(() -> service.criar(form("Maria", CPF, "800.00"), false))
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

        PagamentoAgendado pagamento = service.criar(form, false);

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

        assertThatThrownBy(() -> service.criar(form, false))
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

        service.atualizar(9L, form, false);

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

        assertThatThrownBy(() -> service.atualizar(9L, form, false))
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

        service.lancarRepasse(21L, new BigDecimal("80.00"), SituacaoRepassePagamento.PAGO, null, null);

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
    void repasseParcialGuardaOQueJaFoiPagoEOSaldo() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        ResponsavelPagamento ana = comId(pagamento.getResponsaveis().get(0), 20L);
        when(responsavelPagamentoRepository.findComDetalhesById(20L)).thenReturn(Optional.of(ana));

        service.lancarRepasse(20L, new BigDecimal("300.00"), SituacaoRepassePagamento.PARCIAL,
                new BigDecimal("120.00"), null);

        assertThat(ana.getSituacaoRepasse()).isEqualTo(SituacaoRepassePagamento.PARCIAL);
        assertThat(ana.getValorRepassePago()).isEqualByComparingTo("120.00");
        assertThat(ana.getValorPago()).isEqualByComparingTo("120.00");
        assertThat(ana.getSaldo()).isEqualByComparingTo("180.00");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PagamentoService.ENTIDADE), eq(9L),
                contains("valor já pago do repasse de 'Ana' de sem valor para R$ 120,00"));
    }

    @Test
    void repasseParcialPeloQueFaltaCalculaOPago() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        ResponsavelPagamento ana = comId(pagamento.getResponsaveis().get(0), 20L);
        when(responsavelPagamentoRepository.findComDetalhesById(20L)).thenReturn(Optional.of(ana));

        service.lancarRepasse(20L, new BigDecimal("300.00"), SituacaoRepassePagamento.PARCIAL,
                null, new BigDecimal("100.00"));

        assertThat(ana.getValorRepassePago()).isEqualByComparingTo("200.00");
        assertThat(ana.getSaldo()).isEqualByComparingTo("100.00");
    }

    @Test
    void repasseQuitadoDepoisDoParcialApagaOValorPagoGuardado() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        ResponsavelPagamento ana = comId(pagamento.getResponsaveis().get(0), 20L);
        ana.setValorRepasse(new BigDecimal("300.00"));
        ana.setSituacaoRepasse(SituacaoRepassePagamento.PARCIAL);
        ana.setValorRepassePago(new BigDecimal("120.00"));
        when(responsavelPagamentoRepository.findComDetalhesById(20L)).thenReturn(Optional.of(ana));

        service.lancarRepasse(20L, new BigDecimal("300.00"), SituacaoRepassePagamento.PAGO, null, null);

        assertThat(ana.getValorRepassePago()).isNull();
        assertThat(ana.getValorPago()).isEqualByComparingTo("300.00");
        assertThat(ana.getSaldo()).isEqualByComparingTo("0");
    }

    @Test
    void repasseParcialSemValoresCoerentesERecusado() {
        BigDecimal total = new BigDecimal("300.00");
        SituacaoRepassePagamento parcial = SituacaoRepassePagamento.PARCIAL;

        assertThatThrownBy(() -> service.lancarRepasse(20L, null, parcial, new BigDecimal("100.00"), null))
                .hasMessageContaining("informe o valor total do repasse")
                .extracting("campo").isEqualTo("valor");
        assertThatThrownBy(() -> service.lancarRepasse(20L, total, parcial, null, null))
                .hasMessageContaining("já foi pago ao responsável")
                .extracting("campo").isEqualTo("valorPago");
        assertThatThrownBy(() -> service.lancarRepasse(20L, total, parcial, new BigDecimal("300.00"), null))
                .hasMessageContaining("escolha \"Pago\"")
                .extracting("campo").isEqualTo("valorPago");
        assertThatThrownBy(() -> service.lancarRepasse(20L, total, parcial, null, new BigDecimal("300.00")))
                .hasMessageContaining("escolha \"Pendente\"")
                .extracting("campo").isEqualTo("valorFalta");
        assertThatThrownBy(() -> service.lancarRepasse(20L, total, parcial, new BigDecimal("100.00"),
                new BigDecimal("100.00")))
                .hasMessageContaining("deve dar o repasse")
                .extracting("campo").isEqualTo("valorFalta");
        verifyNoInteractions(responsavelPagamentoRepository);
    }

    @Test
    void responsavelComRepasseParcialNaoSaiDoPagamento() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        ResponsavelPagamento ana = pagamento.getResponsaveis().get(0);
        ana.setValorRepasse(new BigDecimal("100.00"));
        ana.setSituacaoRepasse(SituacaoRepassePagamento.PARCIAL);
        ana.setValorRepassePago(new BigDecimal("40.00"));
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.of(pagamento.getCliente()));
        PagamentoForm form = form("Maria da Silva", CPF, "1500.00");
        form.setResponsavelIds(new ArrayList<>(List.of(3L)));

        assertThatThrownBy(() -> service.atualizar(9L, form, false))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("'Ana' já foi pago em parte");
    }

    @Test
    void trocaDeStatusERegistradaNaAuditoria() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));

        service.alterarStatus(9L, StatusPagamento.RECEBIDO);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.RECEBIDO);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PagamentoService.ENTIDADE), eq(9L),
                contains("recebimento de Agendado para Recebeu"));
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

        service.atualizar(9L, form, false);

        assertThat(pagamento.getValor()).isEqualByComparingTo("1600.00");
        assertThat(pagamento.getBanco()).isEqualTo("Banco do Brasil");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PagamentoService.ENTIDADE), eq(9L),
                contains("banco de Caixa Econômica Federal para Banco do Brasil"));
    }

    @Test
    void edicaoPodeApagarValorEBancoQuandoAindaNaoSeSabe() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.of(pagamento.getCliente()));
        PagamentoForm form = form("Maria da Silva", CPF, "1500.00");
        form.setValor(null);
        form.setBanco(" ");

        service.atualizar(9L, form, false);

        assertThat(pagamento.getValor()).isNull();
        assertThat(pagamento.getBanco()).isNull();
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PagamentoService.ENTIDADE), eq(9L),
                contains("banco de Caixa Econômica Federal para não informado"));
    }

    @Test
    void cadastroJaLancaHonorarioRecebidoERegistraNaCriacao() {
        quandoResponsavelETipoExistem();
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.empty());
        PagamentoForm form = form("Maria da Silva", CPF, "1500.00");
        form.setValorHonorario(new BigDecimal("450.00"));
        form.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);

        PagamentoAgendado pagamento = service.criar(form, true);

        assertThat(pagamento.getValorHonorario()).isEqualByComparingTo("450.00");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.RECEBIDO);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(PagamentoService.ENTIDADE), any(),
                contains("honorário R$ 450,00 (Recebido)"));
    }

    @Test
    void semPermissaoDeHonorarioOCadastroIgnoraOHonorario() {
        quandoResponsavelETipoExistem();
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.empty());
        PagamentoForm form = form("Maria da Silva", CPF, "1500.00");
        form.setValorHonorario(new BigDecimal("450.00"));
        form.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);

        PagamentoAgendado pagamento = service.criar(form, false);

        assertThat(pagamento.getValorHonorario()).isNull();
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.A_RECEBER);
    }

    @Test
    void honorarioRecebidoSemValorERecusadoNoCadastro() {
        PagamentoForm form = form("Maria", CPF, "800.00");
        form.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);

        assertThatThrownBy(() -> service.criar(form, true))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Informe o valor do honorário recebido.")
                .extracting("campo").isEqualTo("valorHonorario");
        verifyNoInteractions(clienteRepository, pagamentoRepository);
    }

    @Test
    void situacaoCalculadaPelaNegociacaoNaoPodeSerEscolhidaNoCadastro() {
        PagamentoForm form = form("Maria", CPF, "800.00");
        form.setValorHonorario(new BigDecimal("300.00"));
        form.setSituacaoHonorario(SituacaoHonorarioPagamento.EM_ATRASO);

        assertThatThrownBy(() -> service.criar(form, true))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("situacaoHonorario");
    }

    @Test
    void cadastroLancaHonorarioParcialComValorJaPago() {
        quandoResponsavelETipoExistem();
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.empty());
        PagamentoForm form = form("Maria da Silva", CPF, "1500.00");
        form.setValorHonorario(new BigDecimal("500.00"));
        form.setSituacaoHonorario(SituacaoHonorarioPagamento.PARCIAL);
        form.setValorHonorarioPago(new BigDecimal("200.00"));

        PagamentoAgendado pagamento = service.criar(form, true);

        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.PARCIAL);
        assertThat(pagamento.getValorHonorarioPago()).isEqualByComparingTo("200.00");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(PagamentoService.ENTIDADE), any(),
                contains("honorário R$ 500,00 (Parcial, já pago R$ 200,00)"));
    }

    @Test
    void cadastroParcialAceitaQuantoFaltaECalculaOValorPago() {
        quandoResponsavelETipoExistem();
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.empty());
        PagamentoForm form = form("Maria da Silva", CPF, "1500.00");
        form.setValorHonorario(new BigDecimal("2000.00"));
        form.setSituacaoHonorario(SituacaoHonorarioPagamento.PARCIAL);
        form.setValorHonorarioFalta(new BigDecimal("550.00"));

        PagamentoAgendado pagamento = service.criar(form, true);

        assertThat(pagamento.getValorHonorarioPago()).isEqualByComparingTo("1450.00");
    }

    @Test
    void parcialPeloQueFaltaRecusaValorQueNaoDeixaNadaPago() {
        PagamentoForm faltaTudo = form("Maria", CPF, "800.00");
        faltaTudo.setValorHonorario(new BigDecimal("300.00"));
        faltaTudo.setSituacaoHonorario(SituacaoHonorarioPagamento.PARCIAL);
        faltaTudo.setValorHonorarioFalta(new BigDecimal("300.00"));

        assertThatThrownBy(() -> service.criar(faltaTudo, true))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("escolha \"A receber\"")
                .extracting("campo").isEqualTo("valorHonorarioFalta");

        PagamentoForm naoFecha = form("Maria", CPF, "800.00");
        naoFecha.setValorHonorario(new BigDecimal("300.00"));
        naoFecha.setSituacaoHonorario(SituacaoHonorarioPagamento.PARCIAL);
        naoFecha.setValorHonorarioPago(new BigDecimal("100.00"));
        naoFecha.setValorHonorarioFalta(new BigDecimal("150.00"));

        assertThatThrownBy(() -> service.criar(naoFecha, true))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Preencha só um dos dois")
                .extracting("campo").isEqualTo("valorHonorarioFalta");
        verifyNoInteractions(clienteRepository, pagamentoRepository);
    }

    @Test
    void honorarioParcialExigeValorJaPagoMenorQueOHonorario() {
        PagamentoForm semPago = form("Maria", CPF, "800.00");
        semPago.setValorHonorario(new BigDecimal("300.00"));
        semPago.setSituacaoHonorario(SituacaoHonorarioPagamento.PARCIAL);

        assertThatThrownBy(() -> service.criar(semPago, true))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Informe quanto o cliente já pagou ou quanto ainda falta pagar do honorário.")
                .extracting("campo").isEqualTo("valorHonorarioPago");

        PagamentoForm pagoTudo = form("Maria", CPF, "800.00");
        pagoTudo.setValorHonorario(new BigDecimal("300.00"));
        pagoTudo.setSituacaoHonorario(SituacaoHonorarioPagamento.PARCIAL);
        pagoTudo.setValorHonorarioPago(new BigDecimal("300.00"));

        assertThatThrownBy(() -> service.criar(pagoTudo, true))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("escolha \"Recebido\"")
                .extracting("campo").isEqualTo("valorHonorarioPago");

        PagamentoForm semHonorario = form("Maria", CPF, "800.00");
        semHonorario.setSituacaoHonorario(SituacaoHonorarioPagamento.PARCIAL);
        semHonorario.setValorHonorarioPago(new BigDecimal("100.00"));

        assertThatThrownBy(() -> service.criar(semHonorario, true))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("valor total do honorário")
                .extracting("campo").isEqualTo("valorHonorario");
        verifyNoInteractions(clienteRepository, pagamentoRepository);
    }

    @Test
    void lancamentoNaLinhaPassaDeParcialParaRecebidoELimpaOValorPago() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        pagamento.setValorHonorario(new BigDecimal("500.00"));
        pagamento.setSituacaoHonorario(SituacaoHonorarioPagamento.PARCIAL);
        pagamento.setValorHonorarioPago(new BigDecimal("200.00"));
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));
        when(negociacaoHonorarioRepository.findByPagamentoIdAndSituacao(9L, SituacaoNegociacao.ATIVA))
                .thenReturn(Optional.empty());

        service.lancarHonorario(9L, new BigDecimal("500.00"), SituacaoHonorarioPagamento.RECEBIDO,
                new BigDecimal("200.00"), null);

        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.RECEBIDO);
        assertThat(pagamento.getValorHonorarioPago()).isNull();
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PagamentoService.ENTIDADE), eq(9L),
                contains("valor já pago do honorário de R$ 200,00 para sem valor, "
                        + "situação do honorário de Parcial para Recebido"));
    }

    @Test
    void edicaoAlteraHonorarioNoMesmoRegistroDeAuditoria() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.of(pagamento.getCliente()));
        when(negociacaoHonorarioRepository.findByPagamentoIdAndSituacao(9L, SituacaoNegociacao.ATIVA))
                .thenReturn(Optional.empty());
        PagamentoForm form = form("Maria da Silva", CPF, "1600.00");
        form.setValorHonorario(new BigDecimal("300.00"));
        form.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);

        service.atualizar(9L, form, true);

        assertThat(pagamento.getValorHonorario()).isEqualByComparingTo("300.00");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.RECEBIDO);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PagamentoService.ENTIDADE), eq(9L),
                contains("valor de R$ 1.500,00 para R$ 1.600,00, honorário de sem valor para R$ 300,00, "
                        + "situação do honorário de A receber para Recebido"));
    }

    @Test
    void edicaoComNegociacaoAtivaNaoMexeNoHonorario() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        pagamento.setValorHonorario(new BigDecimal("900.00"));
        pagamento.setSituacaoHonorario(SituacaoHonorarioPagamento.NEGOCIADO);
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));
        when(clienteRepository.findByCpf(CPF_DIGITOS)).thenReturn(Optional.of(pagamento.getCliente()));
        when(negociacaoHonorarioRepository.findByPagamentoIdAndSituacao(9L, SituacaoNegociacao.ATIVA))
                .thenReturn(Optional.of(mock(NegociacaoHonorario.class)));
        PagamentoForm form = form("Maria da Silva", CPF, "1500.00");

        service.atualizar(9L, form, true);

        assertThat(pagamento.getValorHonorario()).isEqualByComparingTo("900.00");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.NEGOCIADO);
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void excluirApagaOPagamentoERegistraOsDadosNaAuditoria() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        pagamento.setValorHonorario(new BigDecimal("600.00"));
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));

        service.excluir(9L, "Lançado em duplicidade");

        verify(pagamentoRepository).delete(pagamento);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.EXCLUSAO), eq(PagamentoService.ENTIDADE), eq(9L),
                eq("Pagamento de 'Maria da Silva' (Salário-maternidade) de 16/10/2026 excluído. Tinha valor "
                        + "R$ 1.500,00, banco Caixa Econômica Federal, cliente recebeu? Agendado, responsável Ana, "
                        + "honorário R$ 600,00 (A receber). Motivo: Lançado em duplicidade"));
    }

    @Test
    void excluirExigeMotivo() {
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamentoSalvo()));

        assertThatThrownBy(() -> service.excluir(9L, " "))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("motivo da exclusão");
        verify(pagamentoRepository, never()).delete(any());
    }

    @Test
    void naoExcluiPagamentoComHonorarioRecebidoNegociacaoOuRepasse() {
        PagamentoAgendado pagamento = pagamentoSalvo();
        when(pagamentoRepository.findComDetalhesById(9L)).thenReturn(Optional.of(pagamento));

        pagamento.setValorHonorario(new BigDecimal("600.00"));
        pagamento.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);
        assertThatThrownBy(() -> service.excluir(9L, "Lançado em duplicidade"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("\"Recebido\"");

        pagamento.setSituacaoHonorario(SituacaoHonorarioPagamento.A_RECEBER);
        pagamento.getResponsaveis().get(0).setValorRepasse(new BigDecimal("100.00"));
        assertThatThrownBy(() -> service.excluir(9L, "Lançado em duplicidade"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("repasse lançado para 'Ana'");

        pagamento.getResponsaveis().get(0).setValorRepasse(null);
        when(negociacaoHonorarioRepository.existsByPagamentoId(9L)).thenReturn(true);
        assertThatThrownBy(() -> service.excluir(9L, "Lançado em duplicidade"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("negociação de honorário");

        verify(pagamentoRepository, never()).delete(any());
        verifyNoInteractions(auditoriaService);
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
