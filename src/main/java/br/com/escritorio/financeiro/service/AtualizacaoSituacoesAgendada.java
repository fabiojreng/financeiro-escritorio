package br.com.escritorio.financeiro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Atualiza as situações que dependem da data quando a aplicação sobe e logo depois da meia-noite,
 * para cobrir tanto o computador desligado à noite quanto a aplicação ligada direto.
 */
@Component
@ConditionalOnProperty(name = "app.situacoes.atualizacao-automatica", havingValue = "true", matchIfMissing = true)
public class AtualizacaoSituacoesAgendada {

    private static final Logger log = LoggerFactory.getLogger(AtualizacaoSituacoesAgendada.class);

    private final PrevisaoService previsaoService;

    public AtualizacaoSituacoesAgendada(PrevisaoService previsaoService) {
        this.previsaoService = previsaoService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        atualizar();
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "America/Sao_Paulo")
    public void diariamente() {
        atualizar();
    }

    void atualizar() {
        try {
            int alteradas = previsaoService.atualizarSituacoesPelaData();
            if (alteradas > 0) {
                log.info("{} previsão(ões) passaram para Aguardando confirmação porque a data prevista chegou.",
                        alteradas);
            }
        } catch (RuntimeException e) {
            log.error("Não foi possível atualizar as situações das previsões pela data.", e);
        }
    }
}
