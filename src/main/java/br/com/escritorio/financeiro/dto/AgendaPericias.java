package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Pericia;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Perícias do período, já em ordem de dia e horário, e as perícias que ainda aguardam data.
 */
public record AgendaPericias(List<Pericia> pericias, List<Pericia> semData) {

    /** Perícias agrupadas por dia, na ordem da agenda. */
    public Map<LocalDate, List<Pericia>> getPorDia() {
        return pericias.stream()
                .collect(Collectors.groupingBy(Pericia::getData, LinkedHashMap::new, Collectors.toList()));
    }

    public int getQuantidade() {
        return pericias.size();
    }
}
