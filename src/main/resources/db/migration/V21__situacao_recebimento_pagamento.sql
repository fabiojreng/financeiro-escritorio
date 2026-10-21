-- A situação "Pendente / não pago" deixou de existir: quem estava nela continua aguardando o benefício.
-- "Pago" passa a ser "Recebido", porque o que se registra é se o cliente já recebeu o benefício no banco.
alter table pagamento_agendado
    drop constraint ck_pagamento_agendado_status;

update pagamento_agendado set status = 'AGENDADO' where status = 'PENDENTE';
update pagamento_agendado set status = 'RECEBIDO' where status = 'PAGO';

alter table pagamento_agendado
    add constraint ck_pagamento_agendado_status check (status in ('AGENDADO', 'RECEBIDO'));
