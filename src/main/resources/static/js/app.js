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
