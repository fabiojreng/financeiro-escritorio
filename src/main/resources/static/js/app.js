document.addEventListener('submit', function (evento) {
    var mensagem = evento.target.getAttribute('data-confirmar');
    if (mensagem && !window.confirm(mensagem)) {
        evento.preventDefault();
    }
});

// Elementos com data-mostrar-quando="idDoCampo=VALOR" só aparecem quando o campo tem esse valor.
function atualizarCamposCondicionais() {
    document.querySelectorAll('[data-mostrar-quando]').forEach(function (elemento) {
        var partes = elemento.getAttribute('data-mostrar-quando').split('=');
        var campo = document.getElementById(partes[0]);
        if (campo) {
            elemento.hidden = campo.value !== partes[1];
        }
    });
}

document.addEventListener('change', atualizarCamposCondicionais);
document.addEventListener('DOMContentLoaded', atualizarCamposCondicionais);

document.addEventListener('click', function (evento) {
    if (evento.target.closest('[data-imprimir]')) {
        window.print();
    }
});

// Formulários com data-cadastro-rapido="idDoSelect" ficam numa janela (modal) dentro de outro formulário:
// cadastram sem recarregar a página, para não perder o que já foi digitado, e selecionam o item criado.
document.addEventListener('submit', function (evento) {
    var formulario = evento.target;
    var idSelect = formulario.getAttribute('data-cadastro-rapido');
    if (!idSelect) {
        return;
    }
    evento.preventDefault();
    var caixaErro = formulario.querySelector('[data-erro]');
    var botao = formulario.querySelector('[type=submit]');
    caixaErro.hidden = true;
    botao.disabled = true;

    fetch(formulario.action, {
        method: 'POST',
        body: new URLSearchParams(new FormData(formulario)),
        headers: {'Accept': 'application/json'}
    })
        .then(function (resposta) {
            var tipo = resposta.headers.get('Content-Type') || '';
            if (tipo.indexOf('application/json') < 0) {
                throw new Error('Não foi possível cadastrar. Sua sessão pode ter expirado: recarregue a página e tente de novo.');
            }
            return resposta.json();
        })
        .then(function (dados) {
            if (dados.erro) {
                throw new Error(dados.erro);
            }
            var select = document.getElementById(idSelect);
            select.add(new Option(dados.nome, dados.id, true, true));
            select.classList.remove('is-invalid');
            formulario.reset();
            bootstrap.Modal.getOrCreateInstance(formulario.closest('.modal')).hide();
        })
        .catch(function (erro) {
            caixaErro.textContent = erro instanceof TypeError
                ? 'Não foi possível falar com o servidor. Verifique a conexão e tente de novo.'
                : erro.message;
            caixaErro.hidden = false;
        })
        .finally(function () {
            botao.disabled = false;
        });
});

document.addEventListener('shown.bs.modal', function (evento) {
    var formulario = evento.target.querySelector('[data-cadastro-rapido]');
    if (formulario) {
        formulario.querySelector('[data-erro]').hidden = true;
        var primeiroCampo = formulario.querySelector('input:not([type=hidden]), textarea');
        if (primeiroCampo) {
            primeiroCampo.focus();
        }
    }
});
