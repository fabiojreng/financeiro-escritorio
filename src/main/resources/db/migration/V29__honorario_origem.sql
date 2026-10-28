-- Continuação do honorário: um pagamento novo do mesmo cliente e benefício pode continuar o
-- saldo de um pagamento anterior, em vez de abrir outra dívida.
alter table pagamento_agendado
    add column honorario_origem_id bigint references pagamento_agendado (id),
    add constraint ck_pagamento_honorario_origem_nao_auto
        check (honorario_origem_id is null or honorario_origem_id <> id);

create index ix_pagamento_honorario_origem on pagamento_agendado (honorario_origem_id);
create index ix_pagamento_cliente_tipo on pagamento_agendado (cliente_id, tipo_servico_id);
