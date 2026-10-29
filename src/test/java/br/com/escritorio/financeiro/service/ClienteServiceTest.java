package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.dto.ClienteForm;
import br.com.escritorio.financeiro.dto.FiltroSituacao;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
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
class ClienteServiceTest {

    private static final String CPF_VALIDO = "529.982.247-25";

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private ClienteService service;

    @BeforeEach
    void setUp() {
        service = new ClienteService(clienteRepository, auditoriaService);
    }

    @Test
    void criaClienteComCpfSoComDigitosESemCpfNaAuditoria() {
        Cliente criado = service.criar(form("  Maria da Silva ", CPF_VALIDO));

        assertThat(criado.getNome()).isEqualTo("Maria da Silva");
        assertThat(criado.getCpf()).isEqualTo("52998224725");
        assertThat(criado.isAtivo()).isTrue();

        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(ClienteService.ENTIDADE), any(),
                descricao.capture());
        assertThat(descricao.getValue()).contains("Maria da Silva").doesNotContain("52998224725", "529.982");
    }

    @Test
    void cpfEOpcional() {
        Cliente criado = service.criar(form("José", "   "));

        assertThat(criado.getCpf()).isNull();
        verify(clienteRepository, never()).findByCpf(anyString());
    }

    @Test
    void rejeitaCpfComDigitoVerificadorErrado() {
        assertThatThrownBy(() -> service.criar(form("José", "529.982.247-24")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("cpf");
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void rejeitaCpfJaCadastradoInformandoQuemEODono() {
        Cliente existente = cliente(7L, "Maria da Silva", "52998224725");
        when(clienteRepository.findByCpf("52998224725")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.criar(form("Maria Silva", CPF_VALIDO)))
                .isInstanceOfSatisfying(CpfDuplicadoException.class, e -> {
                    assertThat(e.getCampo()).isEqualTo("cpf");
                    assertThat(e.getClienteExistenteId()).isEqualTo(7L);
                    assertThat(e.getClienteExistenteNome()).isEqualTo("Maria da Silva");
                });
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void editarMantendoOProprioCpfEPermitido() {
        Cliente cliente = cliente(7L, "Maria", "52998224725");
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.findByCpf("52998224725")).thenReturn(Optional.of(cliente));

        service.atualizar(7L, form("Maria da Silva", CPF_VALIDO));

        assertThat(cliente.getNome()).isEqualTo("Maria da Silva");
    }

    @Test
    void alteracaoDeCpfRegistraApenasQueMudouSemOValor() {
        Cliente cliente = cliente(7L, "Maria", "52998224725");
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));

        service.atualizar(7L, form("Maria", "111.444.777-35"));

        assertThat(cliente.getCpf()).isEqualTo("11144477735");
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), anyString(), any(), descricao.capture());
        assertThat(descricao.getValue()).contains("CPF").doesNotContain("11144477735", "52998224725");
    }

    @Test
    void cadastroPelaAgendaGravaNomeDaMaeEAOrigemNaAuditoria() {
        when(clienteRepository.findByCpf("52998224725")).thenReturn(Optional.empty());

        Cliente criado = service.obterOuCadastrarPorCpf(" Maria ", CPF_VALIDO, "  Joana  ", LocalDate.of(2026, 10, 27),
                "agenda de perícias", "nomeCliente", "cpf");

        assertThat(criado.getNomeMae()).isEqualTo("Joana");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(ClienteService.ENTIDADE), any(),
                eq("Cliente 'Maria' cadastrado pela agenda de perícias."));
    }

    @Test
    void nomeDaMaeSoCompletaClienteQueAindaNaoTem() {
        Cliente semMae = cliente(7L, "Maria", "52998224725");
        when(clienteRepository.findByCpf("52998224725")).thenReturn(Optional.of(semMae));

        service.obterOuCadastrarPorCpf("Maria", CPF_VALIDO, "Joana", LocalDate.of(2026, 10, 27),
                "agenda de perícias", "nomeCliente", "cpf");
        service.obterOuCadastrarPorCpf("Maria", CPF_VALIDO, "Outra Mãe", LocalDate.of(2026, 10, 27),
                "agenda de perícias", "nomeCliente", "cpf");

        assertThat(semMae.getNomeMae()).isEqualTo("Joana");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(ClienteService.ENTIDADE), eq(7L),
                eq("Cliente 'Maria' alterado: nome da mãe informado pela agenda de perícias."));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void pesquisaPorCpfFormatadoUsaSoOsDigitos() {
        when(clienteRepository.pesquisar(anyString(), anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.pesquisar("529.982", FiltroSituacao.ATIVOS, 0);

        verify(clienteRepository).pesquisar(eq("%529.982%"), eq("%529982%"), eq(""), eq(List.of(true)),
                any(Pageable.class));
    }

    @Test
    void pesquisaPorNomeIgnoraAcentosEMaiusculas() {
        when(clienteRepository.pesquisar(anyString(), anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.pesquisar("  JOÃO ", FiltroSituacao.TODOS, 0);

        verify(clienteRepository).pesquisar(eq("%joao%"), eq(""), eq(""), eq(List.of(true, false)),
                any(Pageable.class));
    }

    @Test
    void pesquisaPorObservacaoIgnoraAcentosEMaiusculas() {
        when(clienteRepository.pesquisar(anyString(), anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.pesquisar("", "  Auxílio NEGADO ", FiltroSituacao.ATIVOS, 0);

        verify(clienteRepository).pesquisar(eq("%%"), eq(""), eq("%auxilio negado%"), eq(List.of(true)),
                any(Pageable.class));
    }

    private ClienteForm form(String nome, String cpf) {
        ClienteForm form = new ClienteForm();
        form.setNome(nome);
        form.setCpf(cpf);
        form.setDataCadastro(LocalDate.of(2026, 10, 1));
        return form;
    }

    private Cliente cliente(Long id, String nome, String cpf) {
        Cliente cliente = new Cliente(nome, LocalDate.of(2026, 10, 1));
        cliente.setCpf(cpf);
        ReflectionTestUtils.setField(cliente, "id", id);
        return cliente;
    }
}
