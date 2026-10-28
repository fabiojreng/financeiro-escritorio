-- A continuação deixa de ter honorário próprio: guarda só o que foi pago ao escritório naquele crédito
-- e repete a situação da dívida de origem.
update pagamento_agendado
set valor_honorario_pago = case situacao_honorario
                               when 'RECEBIDO' then valor_honorario
                               when 'PARCIAL' then valor_honorario_pago
                           end,
    valor_honorario      = null
where honorario_origem_id is not null;

update pagamento_agendado continuacao
set situacao_honorario = origem.situacao_honorario
from pagamento_agendado origem
where continuacao.honorario_origem_id = origem.id;
