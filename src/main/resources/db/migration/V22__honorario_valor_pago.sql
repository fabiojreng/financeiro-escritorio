-- Quanto o cliente já pagou do honorário quando ele está "Parcial" sem negociação.
alter table pagamento_agendado
    add column valor_honorario_pago numeric(15, 2),
    add constraint ck_pagamento_agendado_valor_honorario_pago
        check (valor_honorario_pago is null or valor_honorario_pago > 0);
