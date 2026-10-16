alter table pagamento_agendado
    add column valor_honorario    numeric(15, 2),
    add column situacao_honorario varchar(20) not null default 'A_RECEBER',
    add column valor_repasse      numeric(15, 2),
    add column situacao_repasse   varchar(20) not null default 'PENDENTE',
    add constraint ck_pagamento_agendado_valor_honorario check (valor_honorario is null or valor_honorario > 0),
    add constraint ck_pagamento_agendado_valor_repasse check (valor_repasse is null or valor_repasse > 0),
    add constraint ck_pagamento_agendado_situacao_honorario check (situacao_honorario in ('A_RECEBER', 'RECEBIDO')),
    add constraint ck_pagamento_agendado_situacao_repasse check (situacao_repasse in ('PENDENTE', 'PAGO'));
