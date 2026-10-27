package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.LocalPericia;
import br.com.escritorio.financeiro.domain.Pericia;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoPericia;
import br.com.escritorio.financeiro.domain.TipoPericia;
import br.com.escritorio.financeiro.dto.PericiaForm;
import br.com.escritorio.financeiro.dto.RemarcacaoPericiaForm;
import br.com.escritorio.financeiro.repository.LocalPericiaRepository;
import br.com.escritorio.financeiro.repository.PericiaRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoPericiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PericiaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 27);
    private static final String CPF = "529.982.247-25";

    @Mock
    private PericiaRepository periciaRepository;
    @Mock
    private TipoPericiaRepository tipoPericiaRepository;
    @Mock
    private LocalPericiaRepository localPericiaRepository;
    @Mock
    private ResponsavelRepository responsavelRepository;
    @Mock
    private ClienteService clienteService;
    @Mock
    private AuditoriaService auditoriaService;

    private PericiaService service;
    private Cliente cliente;
    private TipoPericia tipo;
    private Responsavel escritorio;
    private LocalPericia grajau;

    @BeforeEach
    void setUp() {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        Clock relogio = Clock.fixed(HOJE.atStartOfDay(fuso).toInstant(), fuso);
        service = new PericiaService(periciaRepository, tipoPericiaRepository, localPericiaRepository,
                responsavelRepository, clienteService, auditoriaService, relogio);
        cliente = comId(new Cliente("Alleanor Alves Martins Santos", HOJE), 7L);
        tipo = comId(new TipoPericia("Perícia Médica Inicial", null), 2L);
        escritorio = comId(new Responsavel("Escritório", null), 3L);
        grajau = comId(new LocalPericia("Grajaú", null), 4L);
    }

    @Test
    void cadastraComClienteNovoPeloCpfENomeDaMae() {
        when(clienteService.obterOuCadastrarPorCpf(eq("Alleanor Alves Martins Santos"), eq(CPF),
                eq("Raimunda Rodrigues dos Santos"), eq(HOJE), eq("agenda de perícias"), any(), any()))
                .thenReturn(cliente);
        quandoTipoResponsavelELocalExistem();

        PericiaForm form = form(LocalDate.of(2026, 10, 30), LocalTime.of(9, 0));
        form.setNomeMae("Raimunda Rodrigues dos Santos");
        Pericia pericia = service.criar(form);

        assertThat(pericia.getCliente()).isSameAs(cliente);
        assertThat(pericia.getTipoPericia()).isSameAs(tipo);
        assertThat(pericia.getResponsavel()).isSameAs(escritorio);
        assertThat(pericia.getLocal()).isSameAs(grajau);
        assertThat(pericia.getSituacao()).isEqualTo(SituacaoPericia.AGENDADA);
        verify(periciaRepository).save(pericia);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(PericiaService.ENTIDADE), any(),
                contains("30/10/2026 às 09:00"));
    }

    @Test
    void cadastraSemDataHorarioNemLocalParaAgendarDepois() {
        when(clienteService.obterOuCadastrarPorCpf(any(), any(), any(), any(), any(), any(), any())).thenReturn(cliente);
        when(tipoPericiaRepository.findById(2L)).thenReturn(Optional.of(tipo));
        when(responsavelRepository.findById(3L)).thenReturn(Optional.of(escritorio));

        PericiaForm form = form(null, null);
        form.setLocalId(null);
        Pericia pericia = service.criar(form);

        assertThat(pericia.isSemData()).isTrue();
        assertThat(pericia.getLocal()).isNull();
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(PericiaService.ENTIDADE), any(),
                contains("data a definir"));
    }

    @Test
    void rejeitaHorarioSemData() {
        assertThatThrownBy(() -> service.criar(form(null, LocalTime.of(10, 20))))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("horario");
        verify(periciaRepository, never()).save(any());
        verifyNoInteractions(clienteService);
    }

    @Test
    void rejeitaTipoInativoEmPericiaNova() {
        when(clienteService.obterOuCadastrarPorCpf(any(), any(), any(), any(), any(), any(), any())).thenReturn(cliente);
        tipo.setAtivo(false);
        when(tipoPericiaRepository.findById(2L)).thenReturn(Optional.of(tipo));

        assertThatThrownBy(() -> service.criar(form(null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("tipoPericiaId");
    }

    @Test
    void remarcarCriaNovaPericiaLigadaAAnterior() {
        Pericia anterior = agendada(LocalDate.of(2026, 10, 30), LocalTime.of(9, 0));
        when(periciaRepository.findComDetalhesById(10L)).thenReturn(Optional.of(anterior));
        when(auditoriaService.usuarioAtual()).thenReturn("ana");

        RemarcacaoPericiaForm form = new RemarcacaoPericiaForm();
        form.setData(LocalDate.of(2026, 11, 6));
        form.setHorario(LocalTime.of(14, 30));
        form.setLocalId(4L);
        form.setMotivo("INSS remarcou");
        Pericia nova = service.remarcar(10L, form);

        ArgumentCaptor<Pericia> salva = ArgumentCaptor.forClass(Pericia.class);
        verify(periciaRepository).save(salva.capture());
        assertThat(salva.getValue()).isSameAs(nova);
        assertThat(nova.getSituacao()).isEqualTo(SituacaoPericia.AGENDADA);
        assertThat(nova.getData()).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(nova.getHorario()).isEqualTo(LocalTime.of(14, 30));
        assertThat(nova.getLocal()).isSameAs(grajau);
        assertThat(nova.getCliente()).isSameAs(cliente);
        assertThat(nova.getTipoPericia()).isSameAs(tipo);
        assertThat(nova.getResponsavel()).isSameAs(escritorio);

        assertThat(anterior.getSituacao()).isEqualTo(SituacaoPericia.REMARCADA);
        assertThat(anterior.getRemarcadaPara()).isSameAs(nova);
        assertThat(anterior.getMotivoSituacao()).isEqualTo("INSS remarcou");
        assertThat(anterior.getSituacaoAlteradaPor()).isEqualTo("ana");
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PericiaService.ENTIDADE), eq(10L),
                contains("remarcada para 06/11/2026 às 14:30"));
        verifyNoInteractions(localPericiaRepository);
    }

    @Test
    void naoAlteraSituacaoDePericiaQueNaoEstaAgendada() {
        Pericia cancelada = agendada(HOJE.minusDays(1), null);
        when(periciaRepository.findComDetalhesById(10L)).thenReturn(Optional.of(cancelada));
        service.cancelar(10L, "Cliente desistiu");

        assertThatThrownBy(() -> service.marcarRealizada(10L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("cancelada");
        assertThatThrownBy(() -> service.remarcar(10L, new RemarcacaoPericiaForm()))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.atualizar(10L, form(null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pode mais ser editada");
        assertThat(cancelada.getSituacao()).isEqualTo(SituacaoPericia.CANCELADA);
    }

    @Test
    void periciaComDataFuturaNaoPodeSerMarcadaComoRealizada() {
        Pericia futura = agendada(HOJE.plusDays(3), LocalTime.of(9, 30));
        when(periciaRepository.findComDetalhesById(10L)).thenReturn(Optional.of(futura));

        assertThatThrownBy(() -> service.marcarRealizada(10L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("30/10/2026");
        assertThatThrownBy(() -> service.marcarFalta(10L, null))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(futura.isAgendada()).isTrue();
    }

    @Test
    void periciaDeHojePodeSerMarcadaComoRealizada() {
        Pericia deHoje = agendada(HOJE, LocalTime.of(9, 0));
        when(periciaRepository.findComDetalhesById(10L)).thenReturn(Optional.of(deHoje));

        service.marcarRealizada(10L);

        assertThat(deHoje.getSituacao()).isEqualTo(SituacaoPericia.REALIZADA);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CONFIRMACAO), eq(PericiaService.ENTIDADE), eq(10L),
                contains("realizada"));
    }

    @Test
    void periciaSemDataNaoPodeSerMarcadaComoFalta() {
        Pericia semData = agendada(null, null);
        when(periciaRepository.findComDetalhesById(10L)).thenReturn(Optional.of(semData));

        assertThatThrownBy(() -> service.marcarFalta(10L, "Não apareceu"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Informe a data");
    }

    @Test
    void cancelarExigeMotivo() {
        Pericia pericia = agendada(HOJE.plusDays(3), null);
        when(periciaRepository.findComDetalhesById(10L)).thenReturn(Optional.of(pericia));

        assertThatThrownBy(() -> service.cancelar(10L, " "))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("motivo");
        assertThat(pericia.isAgendada()).isTrue();
    }

    @Test
    void edicaoRegistraDataAnteriorENovaNaAuditoria() {
        Pericia pericia = agendada(null, null);
        when(periciaRepository.findComDetalhesById(10L)).thenReturn(Optional.of(pericia));
        when(clienteService.obterEscolhido(eq(7L), any(), any(), eq("agenda de perícias"), any())).thenReturn(cliente);

        PericiaForm form = form(LocalDate.of(2026, 10, 30), LocalTime.of(10, 20));
        form.setClienteId(7L);
        form.setLocalId(null);
        service.atualizar(10L, form);

        assertThat(pericia.getData()).isEqualTo(LocalDate.of(2026, 10, 30));
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), eq(PericiaService.ENTIDADE), eq(10L),
                contains("data de data a definir para 30/10/2026 às 10:20"));
    }

    @Test
    void salvarSemMudancasNaoGeraAuditoria() {
        Pericia pericia = agendada(LocalDate.of(2026, 10, 30), LocalTime.of(9, 0));
        when(periciaRepository.findComDetalhesById(10L)).thenReturn(Optional.of(pericia));
        when(clienteService.obterEscolhido(any(), any(), any(), any(), any())).thenReturn(cliente);

        PericiaForm form = PericiaForm.de(pericia);
        service.atualizar(10L, form);

        verifyNoInteractions(auditoriaService);
    }

    private void quandoTipoResponsavelELocalExistem() {
        when(tipoPericiaRepository.findById(2L)).thenReturn(Optional.of(tipo));
        when(responsavelRepository.findById(3L)).thenReturn(Optional.of(escritorio));
        when(localPericiaRepository.findById(4L)).thenReturn(Optional.of(grajau));
    }

    private Pericia agendada(LocalDate data, LocalTime horario) {
        Pericia pericia = comId(new Pericia(cliente, tipo, escritorio), 10L);
        pericia.setData(data);
        pericia.setHorario(horario);
        pericia.setLocal(grajau);
        return pericia;
    }

    private PericiaForm form(LocalDate data, LocalTime horario) {
        PericiaForm form = new PericiaForm();
        form.setNomeCliente("Alleanor Alves Martins Santos");
        form.setCpf(CPF);
        form.setTipoPericiaId(2L);
        form.setResponsavelId(3L);
        form.setLocalId(4L);
        form.setData(data);
        form.setHorario(horario);
        return form;
    }

    private static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
