import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import vm from "node:vm";
const codigo = readFileSync(
    new URL("../../main/resources/static/js/juego.js", import.meta.url),
    "utf8",
).replace(/^import .*;\r?\n/gm, "");
function preparar() {
    const nodos = new Map(),
        envios = [],
        conexiones = [],
        timers = new Map();
    let handlers,
        errores,
        comunicacionTablero,
        siguiente = 0;
    const ventana = {};
    function crearNodo(id = "") {
        return {
            hidden: true, value: "", textContent: "", resetCount: 0, children: [], listeners: {},
            addEventListener(evento, fn) { this.listeners[evento] = fn; },
            replaceChildren(...hijos) { this.children = hijos; },
            append(...hijos) { this.children.push(...hijos); },
            setAttribute() {},
            querySelectorAll() {
                return id === "tipos" ? this.children.flatMap((e) => e.children.filter((h) => h.tag === "input")) : [];
            },
            reset() {
                this.resetCount++;
                for (const campo of ["nombre", "ancho", "alto", "tacticas"]) nodo(campo).value = "";
            },
        };
    }
    function nodo(id) {
        if (!nodos.has(id))
            nodos.set(id, crearNodo(id));
        return nodos.get(id);
    }
    const dispatcher = {
        mapFunctions(h, e) {
            handlers = h;
            errores = e;
        },
        submit(url, datos) {
            envios.push({ url, datos });
        },
        processCommands(sobre) {
            for (const comando of sobre.commands) handlers[comando.id](comando.parametro);
        },
    };
    class EventSource {
        constructor() {
            conexiones.push(this);
        }
        close() {
            this.cerrada = true;
        }
    }
    vm.runInNewContext(codigo, {
        dispatcher,
        BattleShipBoard: class { constructor(contenedor, comunicacion) { comunicacionTablero = comunicacion; } },
        EventSource,
        document: {
            getElementById: nodo, querySelectorAll: () => [],
            createElement: (tag) => ({ ...crearNodo(), tag }),
            createTextNode: (texto) => ({ textContent: texto }),
        },
        Option: class { constructor(text, value) { this.textContent = text; this.value = value; } },
        window: { addEventListener(evento, fn) { ventana[evento] = fn; } },
        navigator: { sendBeacon() {} },
        URLSearchParams,
        setTimeout(f) {
            timers.set(++siguiente, f);
            return siguiente;
        },
        clearTimeout(id) {
            timers.delete(id);
        },
        encodeURIComponent,
        JSON,
        console,
    });
    return {
        nodo,
        envios,
        conexiones,
        handlers,
        errores,
        ventana,
        dispatcher,
        clic(url = "/api/preparacion/celda?vista=actual&nombre=Mar&version=0") {
            comunicacionTablero.submit(url, { coordenadaVertical: 0, coordenadaHorizontal: 0 });
        },
        actualizar(vista, secuencia, id, parametro, entrada = 1, seleccionOrientacion = 0) {
            dispatcher.processCommands({ commands: [{ id: "actualizar", parametro: {
                vista, secuencia, entrada, seleccionOrientacion, commands: [{ id, parametro }],
            } }] });
        },
        tick() {
            const [id, f] = timers.entries().next().value;
            timers.delete(id);
            f();
        },
        timers,
    };
}
test("recupera la conexión con la batalla actual sin reenviar una jugada", () => {
    const app = preparar();
    app.handlers.batalla({
        nombre: "Atlantico",
        estado: "En combate",
        ronda: 3,
        tactica: "Espiral",
        pendientes: [],
        preparacion: false,
    });
    app.handlers.conectar("anterior");
    app.conexiones[0].onerror();
    app.tick();
    assert.equal(app.envios.at(-1).url, "/api/recuperar");
    assert.equal(app.envios.at(-1).datos.nombre, "Atlantico");
    app.errores.onError(0, "Sin red");
    app.tick();
    assert.equal(app.envios.at(-1).url, "/api/recuperar");
    app.handlers.conectar("nueva");
    assert.equal(app.timers.size, 0);
    app.conexiones[0].onerror();
    assert.equal(app.timers.size, 0, "Una conexión sustituida no debe iniciar otra recuperación");
    assert.ok(app.envios.every((e) => ["/api/abrir", "/api/recuperar"].includes(e.url)));
});
test("un error de validación descarta el formulario y muestra el motivo", () => {
    const app = preparar();
    app.handlers.conectar("actual");
    app.actualizar("actual", 1, "nueva", datosNueva);
    const cantidad = app.nodo("tipos").querySelectorAll()[0];
    cantidad.value = "7";
    app.errores.onException("La cantidad debe estar entre 0 y 6");
    assert.equal(app.nodo("formulario").resetCount, 2);
    assert.equal(cantidad.value, "0");
    assert.equal(app.nodo("error").hidden, false);
    assert.match(app.nodo("error").textContent, /entre 0 y 6/);
});

const datosNueva = { tacticas: ["Secuencial", "Espiral"], tipos: [{ id: "SUBMARINO", nombre: "Submarino", tamano: 2 }] };
const datosSala = (ganadas) => ({ ganadas, perdidas: 0, exito: 0, batallas: [] });
const datosBatalla = (orientacion) => ({ nombre: "Mar", estado: "En preparación", ronda: 1, tactica: "Secuencial", preparacion: true, pendientes: ["Submarino"], orientacion, accion: orientacion });

test("1 nueva, 76 de la vista anterior y 2 nueva termina mostrando la 2", () => {
    const app = preparar();
    app.handlers.conectar("anterior");
    const retirada = app.conexiones[0];
    app.handlers.conectar("nueva");
    app.actualizar("nueva", 1, "sala", datosSala(1));
    app.actualizar("anterior", 76, "sala", datosSala(76));
    assert.equal(app.nodo("ganadas").textContent, 1);
    app.conexiones[1].onmessage({ data: JSON.stringify({ commands: [{ id: "actualizar", parametro: {
        vista: "nueva", secuencia: 2, entrada: 0, seleccionOrientacion: 0, commands: [{ id: "sala", parametro: datosSala(2) }],
    } }] }) });
    assert.equal(app.nodo("ganadas").textContent, 2);
    retirada.onmessage({ data: JSON.stringify({ commands: [{ id: "sala", parametro: datosSala(99) }] }) });
    assert.equal(app.nodo("ganadas").textContent, 2, "El emisor sustituido tampoco aplica comandos");
});

test("reconectar el mismo identificador conserva la última secuencia aceptada", () => {
    const app = preparar();
    app.handlers.conectar("actual");
    app.actualizar("actual", 20, "sala", datosSala(20));
    app.handlers.conectar("actual");
    app.actualizar("actual", 19, "sala", datosSala(19));
    app.actualizar("actual", 20, "sala", datosSala(99));
    assert.equal(app.nodo("ganadas").textContent, 20);
    app.actualizar("actual", 21, "sala", datosSala(21));
    assert.equal(app.nodo("ganadas").textContent, 21);
    assert.ok(app.conexiones[0].cerrada);
});

test("clic captura orientación y revisiones propias de cada vista sin que un refresco las revierta", () => {
    const a = preparar(), b = preparar();
    for (const app of [a,b]) {
        app.handlers.conectar("actual");
        app.actualizar("actual", 1, "batalla", datosBatalla("HORIZONTAL"));
    }
    a.nodo("orientacion").value = "VERTICAL";
    a.nodo("orientacion").listeners.change({ target: a.nodo("orientacion") });
    // No se responde el POST de orientación. Llega otro refresco con selección anterior.
    a.actualizar("actual", 2, "batalla", datosBatalla("HORIZONTAL"));
    assert.equal(a.nodo("orientacion").value, "VERTICAL");
    a.clic(); b.clic();
    assert.equal(a.envios.at(-1).datos.orientacion, "VERTICAL");
    assert.equal(a.envios.at(-1).datos.seleccionOrientacion, 1);
    assert.equal(b.envios.at(-1).datos.orientacion, "HORIZONTAL");
    a.actualizar("actual", 3, "batalla", datosBatalla("VERTICAL"), 1, 1);
    a.nodo("orientacion").value = "HORIZONTAL";
    a.nodo("orientacion").listeners.change({ target: a.nodo("orientacion") });
    a.actualizar("actual", 4, "batalla", datosBatalla("VERTICAL"), 1, 1);
    assert.equal(a.nodo("orientacion").value, "HORIZONTAL");
    a.clic();
    assert.equal(a.envios.at(-1).datos.orientacion, "HORIZONTAL");
    assert.equal(a.envios.at(-1).datos.seleccionOrientacion, 2);
    a.clic("/api/combate/disparar?vista=actual&nombre=Mar&ronda=1");
    assert.equal(a.envios.at(-1).datos.orientacion, undefined, "Un disparo conserva su contrato");
});

test("resincronización de nueva conserva todos los datos; abandonar y volver los descarta", () => {
    const app = preparar();
    app.handlers.conectar("actual");
    app.actualizar("actual", 1, "nueva", datosNueva);
    for (const [campo, valor] of Object.entries({ nombre: "Borrador", ancho: "8", alto: "12", tacticas: "Espiral" })) app.nodo(campo).value = valor;
    const cantidad = app.nodo("tipos").querySelectorAll()[0]; cantidad.value = "3";
    app.conexiones[0].onerror(); app.tick(); app.handlers.conectar("actual");
    app.actualizar("actual", 2, "nueva", datosNueva);
    assert.equal(app.nodo("formulario").resetCount, 1);
    assert.equal(app.nodo("nombre").value, "Borrador");
    assert.equal(app.nodo("ancho").value, "8");
    assert.equal(app.nodo("alto").value, "12");
    assert.equal(app.nodo("tacticas").value, "Espiral");
    assert.equal(app.nodo("tipos").querySelectorAll()[0], cantidad);
    assert.equal(cantidad.value, "3");
    // Incluso si la respuesta de sala se pierde, el número de entrada distingue el nuevo formulario.
    app.actualizar("actual", 4, "nueva", datosNueva, 3);
    assert.equal(app.nodo("formulario").resetCount, 2);
    assert.equal(app.nodo("nombre").value, "");
    assert.equal(app.nodo("tipos").querySelectorAll()[0].value, "0");
});

test("un fallo en la primera apertura se recupera con un solo SSE y sin repetir acciones", () => {
    const app = preparar();
    assert.equal(app.envios[0].url, "/api/abrir");
    app.errores.onError(0, "Sin red");
    assert.equal(app.timers.size, 1);
    app.tick();
    assert.equal(app.envios.at(-1).url, "/api/recuperar");
    app.handlers.conectar("recuperada");
    app.actualizar("recuperada", 1, "sala", datosSala(0), 0);
    assert.equal(app.nodo("sala").hidden, false);
    assert.equal(app.conexiones.filter((e) => !e.cerrada).length, 1);
    assert.equal(app.timers.size, 0);
    app.conexiones[0].onerror();
    app.ventana.pagehide();
    assert.equal(app.timers.size, 0);
    app.handlers.conectar("tardia");
    assert.equal(app.conexiones.length, 1, "Una respuesta posterior al cierre no abre SSE");
    assert.ok(app.envios.every((e) => ["/api/abrir", "/api/recuperar"].includes(e.url)));
});

test("el clic y la selección identifican la entrada actual, incluso al volver a la misma batalla", () => {
    const app = preparar();
    app.handlers.conectar("actual");
    app.actualizar("actual", 1, "batalla", datosBatalla("HORIZONTAL"), 1);
    app.actualizar("actual", 3, "batalla", datosBatalla("HORIZONTAL"), 3);
    app.nodo("orientacion").value = "VERTICAL";
    app.nodo("orientacion").listeners.change({ target: app.nodo("orientacion") });
    assert.equal(app.envios.at(-1).datos.entrada, 3);
    app.clic();
    assert.equal(app.envios.at(-1).datos.entrada, 3);
});

test("una selección sin confirmar no bloquea la leyenda al terminar el despliegue o cambiar de estado", () => {
    for (const [estado, preparacion, accion] of [
        ["En preparación", true, "Batalla preparada"],
        ["En combate", false, "Seleccione una celda de aguas enemigas"],
        ["Ganada", false, "Batalla finalizada"],
        ["Perdida", false, "Batalla finalizada"],
    ]) {
        const app = preparar();
        app.handlers.conectar("actual");
        app.actualizar("actual", 1, "batalla", datosBatalla("HORIZONTAL"));
        app.nodo("orientacion").value = "VERTICAL";
        app.nodo("orientacion").listeners.change({ target: app.nodo("orientacion") });
        app.actualizar("actual", 2, "batalla", { ...datosBatalla("HORIZONTAL"), pendientes: [], estado, preparacion, accion });
        assert.equal(app.nodo("accion").textContent, accion, estado);
        assert.equal(app.nodo("orientacion").value, "VERTICAL", "La leyenda no revierte la selección local");
    }
});

test("la leyenda usa textos del servidor para la selección local y el siguiente tipo colocado desde otra vista", () => {
    const app = preparar();
    app.handlers.conectar("actual");
    const acciones = (barco) => ({
        HORIZONTAL: `Posicione su ${barco} en forma horizontal`,
        VERTICAL: `Posicione su ${barco} en forma vertical`,
    });
    app.actualizar("actual", 1, "batalla", {
        ...datosBatalla("HORIZONTAL"), pendientes: ["Submarino", "Destructor"],
        accion: acciones("Submarino").HORIZONTAL, acciones: acciones("Submarino"),
    });
    app.nodo("orientacion").value = "VERTICAL";
    app.nodo("orientacion").listeners.change({ target: app.nodo("orientacion") });
    assert.equal(app.nodo("accion").textContent, acciones("Submarino").VERTICAL);
    // El POST sigue sin confirmar; otra vista coloca el Submarino y el servidor notifica el Destructor.
    app.actualizar("actual", 2, "batalla", {
        ...datosBatalla("HORIZONTAL"), pendientes: ["Destructor"],
        accion: acciones("Destructor").HORIZONTAL, acciones: acciones("Destructor"),
    });
    assert.equal(app.nodo("orientacion").value, "VERTICAL");
    assert.equal(app.nodo("accion").textContent, acciones("Destructor").VERTICAL);
});
