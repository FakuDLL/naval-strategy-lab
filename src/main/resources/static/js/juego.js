import dispatcher from "./CommandDispatcher.js";
import BattleShipBoard from "./BattleShipBoard.js";

let vista = "",
    nombre = "",
    secuencia = 0,
    eventos;
let entrada,
    formularioEntrada,
    accionesOrientacion = {},
    seleccionOrientacion = 0,
    seleccionRecibida = 0;
let recuperando = true,
    reintento,
    cerrando = false;
const elemento = (id) => document.getElementById(id);
const texto = (id, valor) => {
    elemento(id).textContent = valor;
};
function pantalla(id) {
    for (const p of ["sala", "nueva", "batalla"]) elemento(p).hidden = p !== id;
}
function error(mensaje, descartar = false) {
    // Interpretación literal de CU02: los datos inválidos del primer paso se descartan.
    if (descartar && !elemento("nueva").hidden) {
        elemento("formulario").reset();
        for (const entrada of elemento("tipos").querySelectorAll("input")) entrada.value = "0";
    }
    texto("error", mensaje);
    elemento("error").hidden = false;
}
function recuperarConexion() {
    if (cerrando) return;
    recuperando = true;
    if (eventos) eventos.close();
    clearTimeout(reintento);
    texto("conexion", "Reconectando…");
    reintento = setTimeout(() => dispatcher.submit("/api/recuperar", { vista, nombre }), 3000);
}
function enviar(url, datos = {}) {
    elemento("error").hidden = true;
    return dispatcher.submit(url, { vista, entrada, ...datos });
}
const comunicacionTablero = {
    mapFunctions: (handlers) => dispatcher.mapFunctions(handlers),
    submit: (url, datos) => {
        elemento("error").hidden = true;
        // El widget entrega las coordenadas; la integración captura la selección del clic.
        const colocacion = url.startsWith("/api/preparacion/celda?");
        return dispatcher.submit(url, colocacion ? {
            ...datos,
            entrada,
            orientacion: elemento("orientacion").value,
            seleccionOrientacion,
        } : datos);
    },
};
const tablero = new BattleShipBoard(elemento("tableros"), comunicacionTablero, {
    tamanoFuente: 16,
    tamanoIcono: 22,
    altoMaximo: 460,
});
dispatcher.mapFunctions(
    {
        conectar(id) {
            if (cerrando) return;
            clearTimeout(reintento);
            recuperando = false;
            if (vista !== id) {
                vista = id;
                secuencia = 0;
                entrada = undefined;
                formularioEntrada = undefined;
                seleccionOrientacion = 0;
            }
            if (eventos) eventos.close();
            eventos = new EventSource("/api/eventos?vista=" + encodeURIComponent(vista));
            const conexion = eventos;
            eventos.onmessage = (e) => {
                if (eventos === conexion) dispatcher.processCommands(JSON.parse(e.data));
            };
            eventos.onopen = () => {
                if (eventos === conexion) texto("conexion", "Conectado · Actualización automática");
            };
            eventos.onerror = () => {
                if (eventos === conexion) recuperarConexion();
            };
        },
        actualizar(datos) {
            if (datos.vista !== vista || datos.secuencia <= secuencia) return;
            secuencia = datos.secuencia;
            if (entrada !== datos.entrada) {
                entrada = datos.entrada;
                seleccionOrientacion = 0;
            }
            seleccionRecibida = datos.seleccionOrientacion;
            dispatcher.processCommands({ commands: datos.commands });
        },
        sala(datos) {
            nombre = "";
            pantalla("sala");
            texto("ganadas", datos.ganadas);
            texto("perdidas", datos.perdidas);
            texto("exito", datos.exito + "%");
            const filas = datos.batallas.map((b) => {
                const fila = document.createElement("tr");
                for (const valor of [b.nombre, b.dimensiones, b.estado, b.propia, b.enemiga]) {
                    const celda = document.createElement("td");
                    celda.textContent = valor;
                    fila.append(celda);
                }
                const celda = document.createElement("td"),
                    entrar = document.createElement("button");
                entrar.textContent = "Entrar";
                entrar.addEventListener("click", () => enviar("/api/entrar", { nombre: b.nombre }));
                celda.append(entrar);
                fila.append(celda);
                return fila;
            });
            elemento("batallas").replaceChildren(...filas);
        },
        nueva(datos) {
            nombre = "";
            pantalla("nueva");
            if (formularioEntrada === entrada) return;
            formularioEntrada = entrada;
            elemento("formulario").reset();
            elemento("tacticas").replaceChildren(...datos.tacticas.map((t) => new Option(t, t)));
            elemento("tipos").replaceChildren(
                ...datos.tipos.map((t) => {
                    const etiqueta = document.createElement("label"),
                        entrada = document.createElement("input");
                    etiqueta.append(document.createTextNode(t.nombre + " · " + t.tamano + " celdas"));
                    entrada.name = t.id;
                    entrada.inputMode = "numeric";
                    entrada.value = "0";
                    etiqueta.append(entrada);
                    return etiqueta;
                }),
            );
        },
        batalla(datos) {
            pantalla("batalla");
            nombre = datos.nombre;
            texto("lugar", nombre);
            texto("titulo-batalla", datos.titulo);
            texto("situacion", datos.estado + " · Ronda " + datos.ronda + " · Oponente: " + datos.tactica);
            if (seleccionRecibida >= seleccionOrientacion) {
                elemento("orientacion").value = datos.orientacion;
                seleccionOrientacion = seleccionRecibida;
            }
            accionesOrientacion = datos.acciones || {};
            texto("accion", accionesOrientacion[elemento("orientacion").value] || datos.accion);
            elemento("preparacion").hidden = !datos.preparacion;
            elemento("pendientes").replaceChildren(
                ...datos.pendientes.map((p, i) => {
                    const li = document.createElement("li");
                    li.textContent = p;
                    if (i === 0) li.setAttribute("aria-current", "true");
                    return li;
                }),
            );
        },
    },
    {
        onException: (mensaje) => {
            if (recuperando) {
                nombre = "";
                recuperarConexion();
            } else error(mensaje, true);
        },
        onError: (estado, mensaje) => {
            if (recuperando) recuperarConexion();
            else error("No se pudo completar la solicitud (" + estado + "). " + mensaje);
        },
    },
);
elemento("nueva-batalla").addEventListener("click", () => enviar("/api/nueva"));
for (const boton of document.querySelectorAll(".volver"))
    boton.addEventListener("click", () => enviar("/api/sala"));
elemento("formulario").addEventListener("submit", (e) => {
    e.preventDefault();
    enviar("/api/preparacion/crear", Object.fromEntries(new FormData(e.currentTarget)));
});
elemento("orientacion").addEventListener("change", (e) => {
    const accion = accionesOrientacion[e.target.value];
    if (accion) texto("accion", accion);
    enviar("/api/preparacion/orientacion", {
        nombre,
        orientacion: e.target.value,
        seleccionOrientacion: ++seleccionOrientacion,
    });
});
elemento("iniciar").addEventListener("click", () => enviar("/api/preparacion/iniciar", { nombre }));
window.addEventListener("pagehide", () => {
    cerrando = true;
    clearTimeout(reintento);
    if (eventos) eventos.close();
    if (vista) navigator.sendBeacon("/api/cerrar", new URLSearchParams({ vista }));
});
window.addEventListener("pageshow", (e) => {
    cerrando = false;
    if (e.persisted) recuperarConexion();
});
dispatcher.submit("/api/abrir");
