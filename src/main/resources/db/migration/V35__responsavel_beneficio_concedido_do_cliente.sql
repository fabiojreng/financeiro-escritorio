-- A importação do caderno deixou o benefício sem responsável. Quem já era cliente do escritório
-- tem "de quem é" na perícia; sem perícia, vale o único responsável do pagamento mais recente.
update beneficio_concedido b
set responsavel_id = origem.responsavel_id,
    atualizado_em = now()
from (
    select b2.id,
           coalesce(da_pericia.responsavel_id, do_pagamento.responsavel_id) as responsavel_id
    from beneficio_concedido b2
    left join (
        select distinct on (cliente_id) cliente_id, responsavel_id
        from pericia
        order by cliente_id, (situacao = 'REMARCADA'), data desc nulls last, id desc
    ) da_pericia on da_pericia.cliente_id = b2.cliente_id
    left join (
        select distinct on (pa.cliente_id) pa.cliente_id, pr.responsavel_id
        from pagamento_agendado pa
        join pagamento_responsavel pr on pr.pagamento_agendado_id = pa.id
        where pa.id = (
            select pa2.id
            from pagamento_agendado pa2
            where pa2.cliente_id = pa.cliente_id
            order by pa2.data_recebimento desc, pa2.id desc
            limit 1)
          and (select count(*)
               from pagamento_responsavel pr2
               where pr2.pagamento_agendado_id = pa.id) = 1
        order by pa.cliente_id
    ) do_pagamento on do_pagamento.cliente_id = b2.cliente_id
    where b2.responsavel_id is null
) origem
where b.id = origem.id
  and origem.responsavel_id is not null;
