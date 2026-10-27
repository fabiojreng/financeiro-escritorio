package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.LocalPericia;
import br.com.escritorio.financeiro.domain.Pericia;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoPericia;
import br.com.escritorio.financeiro.domain.TipoPericia;
import br.com.escritorio.financeiro.dto.AgendaPericias;
import br.com.escritorio.financeiro.dto.FiltroPericias;
import br.com.escritorio.financeiro.dto.PericiaForm;
import br.com.escritorio.financeiro.dto.RemarcacaoPericiaForm;
import br.com.escritorio.financeiro.repository.LocalPericiaRepository;
import br.com.escritorio.financeiro.repository.PericiaRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoPericiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Agenda de perícias médicas, que substitui o caderno da recepção. O cliente é encontrado ou cadastrado
 * pelo CPF no próprio formulário, como na agenda de pagamentos.
 */
@Service
public class PericiaService {

    public static final String ENTIDADE = "Perícia";

    private static final String ORIGEM = "agenda de perícias";
    private static final String CAMPO_NOME = "nomeCliente";
    private static final String CAMPO_CPF = "cpf";
    private static final int DIGITOS_MINIMOS_BUSCA_CPF = 3;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final PericiaRepository periciaRepository;
    private final TipoPericiaRepository tipoPericiaRepository;
    private final LocalPericiaRepository localPericiaRepository;
    private final ResponsavelRepository responsavelRepository;
    private final ClienteService clienteService;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public PericiaService(PericiaRepository periciaRepository, TipoPericiaRepository tipoPericiaRepository,
                          LocalPericiaRepository localPericiaRepository, ResponsavelRepository responsavelRepository,
                          ClienteService clienteService, AuditoriaService auditoriaService, Clock relogio) {
        this.periciaRepository = periciaRepository;
        this.tipoPericiaRepository = tipoPericiaRepository;
        this.localPericiaRepository = localPericiaRepository;
        this.responsavelRepository = responsavelRepository;
        this.clienteService = clienteService;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public Pericia buscar(Long id) {
        return periciaRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Perícia não encontrada."));
    }

    /** Perícia que foi remarcada para esta, para mostrar de onde ela veio. */
    @Transactional(readOnly = true)
    public Optional<Pericia> remarcadaDe(Long id) {
        return periciaRepository.findByRemarcadaParaId(id);
    }

    @Transactional(readOnly = true)
    public List<Pericia> doCliente(Long clienteId) {
        return periciaRepository.findByClienteIdOrderByDataDescIdDesc(clienteId);
    }

    @Transactional(readOnly = true)
    public AgendaPericias agenda(FiltroPericias filtro) {
        String termo = filtro.getBusca() == null ? "" : filtro.getBusca().trim();
        String padraoCliente = Textos.padraoBusca(termo);
        String digitos = termo.replaceAll("[\\s.\\-]", "");
        String padraoCpf = digitos.matches("\\d{" + DIGITOS_MINIMOS_BUSCA_CPF + ",11}") ? "%" + digitos + "%" : "";
        List<SituacaoPericia> situacoes = filtro.getSituacao() == null
                ? Arrays.asList(SituacaoPericia.values())
                : List.of(filtro.getSituacao());
        List<Pericia> pericias = periciaRepository.pesquisar(filtro.getInicio(), filtro.getFim(), situacoes,
                padraoCliente, padraoCpf, filtro.getResponsavelId(), filtro.getTipoPericiaId(), filtro.getLocalId());
        List<Pericia> semData = filtro.getSituacao() == null || filtro.getSituacao() == SituacaoPericia.AGENDADA
                ? periciaRepository.pesquisarSemData(padraoCliente, padraoCpf, filtro.getResponsavelId(),
                filtro.getTipoPericiaId(), filtro.getLocalId())
                : List.of();
        return new AgendaPericias(pericias, semData);
    }

    @Transactional(readOnly = true)
    public PericiaForm novoFormulario(LocalDate data, Long clienteId) {
        PericiaForm form = new PericiaForm();
        form.setData(data);
        if (clienteId != null) {
            form.preencherCliente(clienteService.buscar(clienteId));
        }
        return form;
    }

    @Transactional
    public Pericia criar(PericiaForm form) {
        validarDataHorario(form.getData(), form.getHorario(), "horario");
        Cliente cliente = cliente(form);
        Pericia pericia = new Pericia(cliente, tipoAtivo(form.getTipoPericiaId()),
                responsavelAtivo(form.getResponsavelId()));
        pericia.setData(form.getData());
        pericia.setHorario(form.getHorario());
        pericia.setLocal(form.getLocalId() == null ? null : localAtivo(form.getLocalId(), "localId"));
        pericia.setObservacoes(Textos.opcional(form.getObservacoes()));
        periciaRepository.save(pericia);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, pericia.getId(),
                "Perícia de '" + cliente.getNome() + "' (" + pericia.getTipoPericia().getNome() + ") cadastrada para "
                        + quando(pericia) + ".");
        return pericia;
    }

    @Transactional
    public Pericia atualizar(Long id, PericiaForm form) {
        Pericia pericia = buscar(id);
        exigirAgendada(pericia, "editada");
        validarDataHorario(form.getData(), form.getHorario(), "horario");

        List<String> alteracoes = new ArrayList<>();
        Cliente cliente = cliente(form);
        if (!pericia.getCliente().equals(cliente)) {
            alteracoes.add("cliente de '" + pericia.getCliente().getNome() + "' para '" + cliente.getNome() + "'");
            pericia.setCliente(cliente);
        }
        if (!pericia.getTipoPericia().getId().equals(form.getTipoPericiaId())) {
            TipoPericia tipo = tipoAtivo(form.getTipoPericiaId());
            alteracoes.add("tipo de '" + pericia.getTipoPericia().getNome() + "' para '" + tipo.getNome() + "'");
            pericia.setTipoPericia(tipo);
        }
        if (!pericia.getResponsavel().getId().equals(form.getResponsavelId())) {
            Responsavel responsavel = responsavelAtivo(form.getResponsavelId());
            alteracoes.add("responsável de '" + pericia.getResponsavel().getNome() + "' para '"
                    + responsavel.getNome() + "'");
            pericia.setResponsavel(responsavel);
        }
        Long localAtual = pericia.getLocal() == null ? null : pericia.getLocal().getId();
        if (!Objects.equals(localAtual, form.getLocalId())) {
            LocalPericia local = form.getLocalId() == null ? null : localAtivo(form.getLocalId(), "localId");
            alteracoes.add("local de " + nomeLocal(pericia.getLocal()) + " para " + nomeLocal(local));
            pericia.setLocal(local);
        }
        if (!Objects.equals(pericia.getData(), form.getData())
                || !Objects.equals(pericia.getHorario(), form.getHorario())) {
            String anterior = quando(pericia);
            pericia.setData(form.getData());
            pericia.setHorario(form.getHorario());
            alteracoes.add("data de " + anterior + " para " + quando(pericia));
        }
        String observacoes = Textos.opcional(form.getObservacoes());
        if (!Objects.equals(pericia.getObservacoes(), observacoes)) {
            alteracoes.add("observações");
            pericia.setObservacoes(observacoes);
        }

        if (!alteracoes.isEmpty()) {
            registrarAlteracao(pericia, String.join(", ", alteracoes));
        }
        return pericia;
    }

    /**
     * Encerra a perícia atual como remarcada e cria outra, agendada para a nova data, com o mesmo cliente,
     * tipo e responsável. As duas ficam ligadas para o histórico continuar visível.
     */
    @Transactional
    public Pericia remarcar(Long id, RemarcacaoPericiaForm form) {
        Pericia pericia = buscar(id);
        exigirAgendada(pericia, "remarcada");
        if (form.getData() == null) {
            throw new RegraNegocioException("data", "Informe a nova data da perícia.");
        }
        LocalPericia local = form.getLocalId() == null ? null : localParaRemarcacao(pericia, form.getLocalId());
        Pericia nova = pericia.remarcacao(form.getData(), form.getHorario(), local);
        periciaRepository.save(nova);

        String anterior = quando(pericia);
        String motivo = Textos.opcional(form.getMotivo());
        pericia.marcarRemarcada(nova, motivo, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        String detalheMotivo = motivo == null ? "" : " Motivo: " + motivo;
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, pericia.getId(),
                "Perícia de '" + pericia.getCliente().getNome() + "' de " + anterior + " remarcada para "
                        + quando(nova) + "." + detalheMotivo);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, nova.getId(),
                "Perícia de '" + nova.getCliente().getNome() + "' agendada para " + quando(nova)
                        + " (remarcação da perícia de " + anterior + ").");
        return nova;
    }

    @Transactional
    public Pericia marcarRealizada(Long id) {
        Pericia pericia = buscar(id);
        exigirAgendada(pericia, "marcada como realizada");
        exigirDataPassada(pericia, "realizada");
        pericia.marcarRealizada(auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        auditoriaService.registrar(AcaoAuditoria.CONFIRMACAO, ENTIDADE, pericia.getId(),
                "Perícia de '" + pericia.getCliente().getNome() + "' de " + quando(pericia) + " marcada como realizada.");
        return pericia;
    }

    @Transactional
    public Pericia marcarFalta(Long id, String motivo) {
        Pericia pericia = buscar(id);
        exigirAgendada(pericia, "marcada como falta");
        exigirDataPassada(pericia, "marcada como falta");
        String observacao = Textos.opcional(motivo);
        if (observacao != null && observacao.length() > MotivoCancelamento.TAMANHO_MAXIMO) {
            throw new RegraNegocioException("motivo",
                    "O motivo pode ter no máximo " + MotivoCancelamento.TAMANHO_MAXIMO + " caracteres.");
        }
        pericia.marcarFalta(observacao, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, pericia.getId(),
                "Cliente '" + pericia.getCliente().getNome() + "' faltou à perícia de " + quando(pericia) + "."
                        + (observacao == null ? "" : " Motivo: " + observacao));
        return pericia;
    }

    @Transactional
    public Pericia cancelar(Long id, String motivo) {
        Pericia pericia = buscar(id);
        String motivoValidado = MotivoCancelamento.validar(motivo);
        exigirAgendada(pericia, "cancelada");
        pericia.cancelar(motivoValidado, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        auditoriaService.registrar(AcaoAuditoria.CANCELAMENTO, ENTIDADE, pericia.getId(),
                "Perícia de '" + pericia.getCliente().getNome() + "' de " + quando(pericia) + " cancelada. Motivo: "
                        + motivoValidado);
        return pericia;
    }

    private static void validarDataHorario(LocalDate data, LocalTime horario, String campoHorario) {
        if (horario != null && data == null) {
            throw new RegraNegocioException(campoHorario, "Informe a data para poder informar o horário.");
        }
    }

    private static void exigirAgendada(Pericia pericia, String acao) {
        if (!pericia.isAgendada()) {
            throw new RegraNegocioException("Esta perícia está " + pericia.getSituacao().getRotulo().toLowerCase()
                    + " e não pode mais ser " + acao + ".");
        }
    }

    private void exigirDataPassada(Pericia pericia, String acao) {
        if (pericia.getData() == null) {
            throw new RegraNegocioException("Informe a data da perícia antes de marcá-la como " + acao + ".");
        }
        if (pericia.getData().isAfter(hoje())) {
            throw new RegraNegocioException("A perícia está marcada para " + Textos.data(pericia.getData())
                    + " e ainda não pode ser " + acao + ".");
        }
    }

    private Cliente cliente(PericiaForm form) {
        if (form.getClienteId() != null) {
            return clienteService.obterEscolhido(form.getClienteId(), form.getCpf(), form.getNomeMae(), ORIGEM,
                    CAMPO_CPF);
        }
        return clienteService.obterOuCadastrarPorCpf(form.getNomeCliente(), form.getCpf(), form.getNomeMae(), hoje(),
                ORIGEM, CAMPO_NOME, CAMPO_CPF);
    }

    private TipoPericia tipoAtivo(Long id) {
        TipoPericia tipo = tipoPericiaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("tipoPericiaId", "Tipo de perícia não encontrado."));
        if (!tipo.isAtivo()) {
            throw new RegraNegocioException("tipoPericiaId",
                    "O tipo '" + tipo.getNome() + "' está inativo e não pode ser usado em novas perícias.");
        }
        return tipo;
    }

    private Responsavel responsavelAtivo(Long id) {
        Responsavel responsavel = responsavelRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("responsavelId", "Responsável não encontrado."));
        if (!responsavel.isAtivo()) {
            throw new RegraNegocioException("responsavelId",
                    "O responsável '" + responsavel.getNome() + "' está inativo e não pode ser usado em novos registros.");
        }
        return responsavel;
    }

    private LocalPericia localAtivo(Long id, String campo) {
        LocalPericia local = localPericiaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException(campo, "Local de perícia não encontrado."));
        if (!local.isAtivo()) {
            throw new RegraNegocioException(campo,
                    "O local '" + local.getNome() + "' está inativo e não pode ser usado em novas perícias.");
        }
        return local;
    }

    /** Na remarcação, o local atual continua valendo mesmo que tenha sido desativado. */
    private LocalPericia localParaRemarcacao(Pericia pericia, Long localId) {
        if (pericia.getLocal() != null && pericia.getLocal().getId().equals(localId)) {
            return pericia.getLocal();
        }
        return localAtivo(localId, "localId");
    }

    private void registrarAlteracao(Pericia pericia, String alteracoes) {
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, pericia.getId(),
                "Perícia de '" + pericia.getCliente().getNome() + "' alterada: " + alteracoes + ".");
    }

    private static String nomeLocal(LocalPericia local) {
        return local == null ? "não informado" : "'" + local.getNome() + "'";
    }

    /** Data e horário como "30/10/2026 às 09:00", ou "data a definir". */
    private static String quando(Pericia pericia) {
        if (pericia.getData() == null) {
            return "data a definir";
        }
        String data = Textos.data(pericia.getData());
        return pericia.getHorario() == null ? data : data + " às " + HORA.format(pericia.getHorario());
    }
}
