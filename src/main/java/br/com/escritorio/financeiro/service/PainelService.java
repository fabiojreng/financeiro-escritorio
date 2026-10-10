package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.domain.SituacaoRepasse;
import br.com.escritorio.financeiro.dto.PainelIndicadores;
import br.com.escritorio.financeiro.dto.PainelIndicadores.ValoresCliente;
import br.com.escritorio.financeiro.dto.PainelIndicadores.ValoresEscritorio;
import br.com.escritorio.financeiro.repository.AcordoHonorariosRepository;
import br.com.escritorio.financeiro.repository.PrevisaoBeneficioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import br.com.escritorio.financeiro.repository.RepasseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** Indicadores do painel inicial. O significado de cada número está em {@link PainelIndicadores}. */
@Service
public class PainelService {

    private final PrevisaoBeneficioRepository previsaoRepository;
    private final AcordoHonorariosRepository acordoRepository;
    private final RecebimentoHonorarioRepository recebimentoRepository;
    private final RepasseRepository repasseRepository;
    private final Clock relogio;

    public PainelService(PrevisaoBeneficioRepository previsaoRepository, AcordoHonorariosRepository acordoRepository,
                         RecebimentoHonorarioRepository recebimentoRepository, RepasseRepository repasseRepository,
                         Clock relogio) {
        this.previsaoRepository = previsaoRepository;
        this.acordoRepository = acordoRepository;
        this.recebimentoRepository = recebimentoRepository;
        this.repasseRepository = repasseRepository;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public PainelIndicadores calcular(LocalDate inicio, LocalDate fim) {
        return new PainelIndicadores(valoresCliente(inicio, fim), valoresEscritorio(inicio, fim));
    }

    private ValoresCliente valoresCliente(LocalDate inicio, LocalDate fim) {
        LocalDate hoje = hoje();
        LocalDate ultimoDiaAtrasado = hoje.minusDays(1).isBefore(fim) ? hoje.minusDays(1) : fim;
        long atrasadasNoPeriodo = ultimoDiaAtrasado.isBefore(inicio) ? 0
                : previsaoRepository.countBySituacaoInAndDataPrevistaBetween(SituacaoPrevisao.PENDENTES, inicio,
                ultimoDiaAtrasado);
        return new ValoresCliente(
                previsaoRepository.countByDataPrevistaBetweenAndSituacaoNot(inicio, fim, SituacaoPrevisao.CANCELADO),
                previsaoRepository.somarEstimadoNaoCancelado(inicio, fim),
                previsaoRepository.somarConfirmadoPorDataEfetiva(inicio, fim),
                previsaoRepository.countBySituacaoInAndDataPrevistaBetween(SituacaoPrevisao.PENDENTES, inicio, fim),
                atrasadasNoPeriodo,
                previsaoRepository.countBySituacaoInAndDataPrevistaBefore(SituacaoPrevisao.PENDENTES, hoje));
    }

    private ValoresEscritorio valoresEscritorio(LocalDate inicio, LocalDate fim) {
        return new ValoresEscritorio(
                acordoRepository.somarContratadoEntre(inicio, fim),
                acordoRepository.countByDataAcordoBetweenAndValorContratadoIsNull(inicio, fim),
                recebimentoRepository.somarRecebidoEntre(inicio, fim),
                acordoRepository.somarContratadoAte(fim)
                        .subtract(recebimentoRepository.somarRecebidoEmAcordosComValorAte(fim)),
                repasseRepository.somarPendentesAte(fim),
                repasseRepository.countBySituacaoAndDataPrevistaLessThanEqual(SituacaoRepasse.PENDENTE, fim),
                repasseRepository.somarPagosEntre(inicio, fim));
    }
}
