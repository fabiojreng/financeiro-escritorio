-- O nome do cliente passa a ser gravado sempre em maiúsculas. O upper() sozinho depende do
-- idioma do banco e pode deixar o acento minúsculo; o translate cobre as letras do português.
update cliente
set nome = translate(upper(nome),
    'áàâãäéèêëíìîïóòôõöúùûüçñ',
    'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ')
where nome is distinct from translate(upper(nome),
    'áàâãäéèêëíìîïóòôõöúùûüçñ',
    'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ');
