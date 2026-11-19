package dev.navalstrategylab;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import dev.navalstrategylab.modelo.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JuegoWebTest {
    @LocalServerPort int puerto;
    @Autowired Fachada fachada;
    private final HttpClient cliente=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final JsonMapper json=new JsonMapper();
    private final List<String> vistas=new ArrayList<>();
    private final Map<String,Long> entradas=new HashMap<>();
    @AfterEach void cerrar() throws Exception {
        for(String vista:vistas) post("/api/cerrar",Map.of("vista",vista));
        cliente.shutdownNow();
    }
    private URI uri(String ruta) { return URI.create("http://localhost:"+puerto+ruta); }
    private HttpResponse<String> post(String ruta,Map<String,?> datos) throws Exception {
        Map<String,Object> parametros=new HashMap<>(datos);
        if(ruta.equals("/api/preparacion/celda")||ruta.equals("/api/preparacion/orientacion"))
            parametros.putIfAbsent("entrada",entradas.getOrDefault(datos.get("vista"),0L));
        StringJoiner form=new StringJoiner("&");
        parametros.forEach((k,v)->form.add(URLEncoder.encode(k,StandardCharsets.UTF_8)+"="+URLEncoder.encode(v.toString(),StandardCharsets.UTF_8)));
        var respuesta=cliente.send(HttpRequest.newBuilder(uri(ruta)).timeout(Duration.ofSeconds(5))
                .header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form.toString())).build(),HttpResponse.BodyHandlers.ofString());
        if(respuesta.statusCode()==200&&!respuesta.body().isBlank()) {
            JsonNode sobre=contenido(respuesta.body());
            if(sobre.has("entrada"))entradas.put(sobre.path("vista").asText(),sobre.path("entrada").asLong());
        }
        return respuesta;
    }
    private String abrir() throws Exception {
        HttpResponse<String> respuesta=post("/api/abrir",Map.of());assertEquals(200,respuesta.statusCode());
        String vista=json.readTree(respuesta.body()).path("commands").get(0).path("parametro").asText();
        vistas.add(vista);return vista;
    }
    private JsonNode contenido(String cuerpo) {return json.readTree(cuerpo).path("commands").get(0).path("parametro");}
    private JsonNode comando(JsonNode sobre,String id) {
        for(JsonNode c:sobre.path("commands")) if(c.path("id").asText().equals(id)) return c.path("parametro");
        fail("Falta comando "+id);return null;
    }
    private String crear(String vista) throws Exception {
        String nombre="Web-"+UUID.randomUUID();
        HttpResponse<String> respuesta=post("/api/preparacion/crear",Map.of("vista",vista,"nombre",nombre,"ancho",6,"alto",6,"tactica","Secuencial","SUBMARINO",1));
        assertEquals(200,respuesta.statusCode(),respuesta.body());return nombre;
    }
    private final class Flujo implements AutoCloseable {
        final InputStream entrada;
        final BlockingQueue<String> mensajes=new LinkedBlockingQueue<>();
        Flujo(String vista) throws Exception {
            HttpResponse<InputStream> respuesta=cliente.send(HttpRequest.newBuilder(uri("/api/eventos?vista="+vista)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
            assertEquals(200,respuesta.statusCode());entrada=respuesta.body();
            Thread.startVirtualThread(()->{
                try(BufferedReader lector=new BufferedReader(new InputStreamReader(entrada,StandardCharsets.UTF_8))) {
                    String linea;while((linea=lector.readLine())!=null) if(linea.startsWith("data:")) mensajes.add(linea.substring(5));
                } catch(IOException cerrado) { }
            });
        }
        JsonNode siguiente() throws Exception {
            String mensaje=mensajes.poll(5,TimeUnit.SECONDS);assertNotNull(mensaje,"No llegó actualización SSE");return contenido(mensaje);
        }
        public void close() throws IOException {entrada.close();}
    }
    @Test void raizFormularioErroresYOcultacionEnemiga() throws Exception {
        var pagina=cliente.send(HttpRequest.newBuilder(uri("/")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,pagina.statusCode());assertTrue(pagina.body().contains("Sala de operaciones"));
        String vista=abrir();int antes=fachada.consultarSala().getTotal();
        assertEquals(299,post("/api/preparacion/crear",Map.of("vista",vista,"ancho","abc","alto",6,"tactica","Secuencial")).statusCode());
        assertEquals(antes,fachada.consultarSala().getTotal());
        String nombre=crear(vista);
        JsonNode entrada=contenido(post("/api/entrar",Map.of("vista",vista,"nombre",nombre)).body());
        assertEquals("En preparación",comando(entrada,"batalla").path("estado").asText());
        assertTrue(entrada.toString().contains("escuchadorOceanoEnemigo"));
        for(JsonNode c:entrada.path("commands")) assertNotEquals("mostrarAguasEnemigas",c.path("id").asText());
        assertEquals(299,post("/api/preparacion/iniciar",Map.of("vista",vista,"nombre",nombre)).statusCode());
        assertEquals(299,post("/api/combate/disparar",Map.of("vista",vista,"nombre",nombre,"ronda",1,"coordenadaVertical",0,"coordenadaHorizontal",0)).statusCode());
        assertEquals(0,fachada.consultarBatalla(nombre).getVersion());
        assertEquals(200,post("/api/preparacion/celda",Map.of("vista",vista,"nombre",nombre,"version",0,"coordenadaVertical",5,"coordenadaHorizontal",0,"orientacion","HORIZONTAL")).statusCode());
        assertEquals(200,post("/api/preparacion/iniciar",Map.of("vista",vista,"nombre",nombre)).statusCode());
        var aguasPropias=post("/api/preparacion/celda",Map.of("vista",vista,"nombre",nombre,"version",2,"coordenadaVertical",0,"coordenadaHorizontal",0,"orientacion","HORIZONTAL"));
        assertEquals(299,aguasPropias.statusCode());
        assertEquals("No puede disparar en aguas propias",aguasPropias.body());
        assertEquals(2,fachada.consultarBatalla(nombre).getVersion());
    }
    @Test void dosVistasRecibenRondaCompletaYReconectanSinMezclarBatallas() throws Exception {
        String a=abrir(), b=abrir(), otra=abrir(), sala=abrir();String nombre=crear(a);
        fachada.colocarBarco(nombre,0,new Coordenada(5,0),Orientacion.HORIZONTAL);fachada.iniciarBatalla(nombre);
        post("/api/entrar",Map.of("vista",b,"nombre",nombre));
        post("/api/entrar",Map.of("vista",otra,"nombre","Indico"));
        try(Flujo fa=new Flujo(a);Flujo fb=new Flujo(b);Flujo fo=new Flujo(otra);Flujo fs=new Flujo(sala)) {
            JsonNode inicial=fa.siguiente();fb.siguiente();fo.siguiente();fs.siguiente();
            assertEquals(a, inicial.path("vista").asText());
            var respuesta=post("/api/combate/disparar",Map.of("vista",a,"nombre",nombre,"ronda",1,"coordenadaVertical",0,"coordenadaHorizontal",0));
            assertEquals(200,respuesta.statusCode(),respuesta.body());
            JsonNode ea=fa.siguiente(),eb=fb.siguiente();
            assertEquals(a, ea.path("vista").asText());
            assertEquals(b, eb.path("vista").asText());
            assertTrue(ea.path("secuencia").asLong()>inicial.path("secuencia").asLong());
            for(JsonNode e:List.of(ea,eb)) {
                assertEquals(2,comando(e,"batalla").path("ronda").asInt());
                long enemigos=0,propios=0;
                for(JsonNode c:e.path("commands")) {
                    if(c.path("id").asText().equals("mostrarAguasEnemigas")) enemigos++;
                    if(c.path("id").asText().equals("mostrarAguasPropias")&&!c.path("parametro").path("valor").asText().equals("barco")) propios++;
                }
                assertEquals(1,enemigos);assertEquals(1,propios);
            }
            assertTrue(comando(fs.siguiente(),"sala").path("batallas").size()>=4);
            assertNull(fo.mensajes.poll(150,TimeUnit.MILLISECONDS));
            assertEquals(299,post("/api/combate/disparar",Map.of("vista",b,"nombre",nombre,"ronda",1,"coordenadaVertical",0,"coordenadaHorizontal",1)).statusCode());
            assertEquals(1,fachada.consultarBatalla(nombre).getEnemigo().getDisparos().size());
            try(Flujo reconexion=new Flujo(a)) {
                assertEquals(2,comando(reconexion.siguiente(),"batalla").path("ronda").asInt());
                assertEquals(200,post("/api/combate/disparar",Map.of("vista",b,"nombre",nombre,"ronda",2,"coordenadaVertical",0,"coordenadaHorizontal",1)).statusCode());
                assertEquals(nombre,comando(reconexion.siguiente(),"batalla").path("nombre").asText());
            }
        }
    }
    @Test void navegacionInvalidaNoUsaBatallaDeOtraPantallaYDerrotaRevela() throws Exception {
        String vista=abrir();
        JsonNode perdida=contenido(post("/api/entrar",Map.of("vista",vista,"nombre","Artico")).body());
        assertEquals("Perdida",comando(perdida,"batalla").path("estado").asText());
        boolean revelado=false;
        for(JsonNode c:perdida.path("commands")) if(c.path("id").asText().equals("mostrarAguasEnemigas")&&c.path("parametro").path("valor").asText().equals("barco")) revelado=true;
        assertTrue(revelado);
        post("/api/sala",Map.of("vista",vista));
        assertEquals(299,post("/api/combate/disparar",Map.of("vista",vista,"nombre","Artico","ronda",1,"coordenadaVertical",0,"coordenadaHorizontal",0)).statusCode());
        post("/api/cerrar",Map.of("vista",vista));
        assertEquals(299,post("/api/entrar",Map.of("vista",vista,"nombre","Artico")).statusCode());
    }
    @Test void recuperaVistaEliminadaSinRepetirAccionesDelJuego() throws Exception {
        String anterior = abrir();
        String nombre = crear(anterior);
        long version = fachada.consultarBatalla(nombre).getVersion();
        post("/api/cerrar", Map.of("vista", anterior));
        var respuesta = post("/api/recuperar", Map.of("vista", anterior, "nombre", nombre));
        assertEquals(200, respuesta.statusCode());
        String nueva = contenido(respuesta.body()).asText();
        vistas.add(nueva);
        assertNotEquals(anterior, nueva);
        try (Flujo flujo = new Flujo(nueva)) {
            assertEquals(nombre, comando(flujo.siguiente(), "batalla").path("nombre").asText());
        }
        assertEquals(version, fachada.consultarBatalla(nombre).getVersion());
        assertEquals(nueva, contenido(post("/api/recuperar", Map.of("vista", nueva, "nombre", nombre)).body()).asText());
    }

    @Test void clicUsaSuOrientacionAunqueLlegueAntesDelCambioYLasVistasSonIndependientes() throws Exception {
        String a=abrir(), b=abrir();
        String nombre="Orientacion-"+UUID.randomUUID();
        assertEquals(200,post("/api/preparacion/crear",Map.of("vista",a,"nombre",nombre,"ancho",8,"alto",8,"tactica","Secuencial","SUBMARINO",3)).statusCode());
        post("/api/entrar",Map.of("vista",b,"nombre",nombre));
        // El clic vertical llega primero: la selección guardada todavía era horizontal.
        var clic=post("/api/preparacion/celda",Map.of("vista",a,"nombre",nombre,"version",0,"coordenadaVertical",0,"coordenadaHorizontal",0,"orientacion","VERTICAL","seleccionOrientacion",1));
        assertEquals(200,clic.statusCode(),clic.body());
        assertEquals(a,contenido(clic.body()).path("vista").asText());
        var acciones=comando(contenido(clic.body()),"batalla").path("acciones");
        assertEquals("Posicione su Submarino en forma horizontal",acciones.path("HORIZONTAL").asText());
        assertEquals("Posicione su Submarino en forma vertical",acciones.path("VERTICAL").asText());
        assertEquals(List.of(new Coordenada(0,0),new Coordenada(1,0)),fachada.consultarBatalla(nombre).getPropio().getCeldasBarcos());
        assertEquals(2,fachada.consultarBatalla(nombre).getPropio().getPendientes().size());
        post("/api/preparacion/orientacion",Map.of("vista",a,"nombre",nombre,"orientacion","VERTICAL","seleccionOrientacion",1));
        try(Flujo fb=new Flujo(b)) {
            assertEquals("HORIZONTAL",comando(fb.siguiente(),"batalla").path("orientacion").asText());
        }
        // La segunda selección se confirma antes de una petición vieja de la primera.
        post("/api/preparacion/orientacion",Map.of("vista",a,"nombre",nombre,"orientacion","HORIZONTAL","seleccionOrientacion",2));
        JsonNode atrasada=contenido(post("/api/preparacion/orientacion",Map.of("vista",a,"nombre",nombre,"orientacion","VERTICAL","seleccionOrientacion",1)).body());
        assertEquals("HORIZONTAL",comando(atrasada,"batalla").path("orientacion").asText());
        assertEquals(2,atrasada.path("seleccionOrientacion").asLong());
        assertEquals(200,post("/api/preparacion/celda",Map.of("vista",a,"nombre",nombre,"version",1,"coordenadaVertical",4,"coordenadaHorizontal",0,"orientacion","HORIZONTAL","seleccionOrientacion",2)).statusCode());
        assertTrue(fachada.consultarBatalla(nombre).getPropio().getCeldasBarcos().containsAll(List.of(new Coordenada(4,0),new Coordenada(4,1))));
        // B elige vertical sin modificar la selección horizontal de A.
        assertEquals(200,post("/api/preparacion/celda",Map.of("vista",b,"nombre",nombre,"version",2,"coordenadaVertical",0,"coordenadaHorizontal",5,"orientacion","VERTICAL","seleccionOrientacion",1)).statusCode());
        try(Flujo fa=new Flujo(a); Flujo fb=new Flujo(b)) {
            var pantallaA=comando(fa.siguiente(),"batalla");
            assertEquals("HORIZONTAL",pantallaA.path("orientacion").asText());
            assertEquals("Batalla preparada",pantallaA.path("acciones").path("VERTICAL").asText());
            assertEquals("VERTICAL",comando(fb.siguiente(),"batalla").path("orientacion").asText());
        }
        var actual=fachada.consultarBatalla(nombre);
        assertEquals(3,actual.getVersion());
        assertTrue(actual.getPropio().getPendientes().isEmpty());
        assertTrue(actual.getPropio().getCeldasBarcos().containsAll(List.of(new Coordenada(0,5),new Coordenada(1,5))));
        // Se conserva rechazo por versión y pertenencia a la vista, sin consumo adicional.
        assertEquals(299,post("/api/preparacion/celda",Map.of("vista",a,"nombre",nombre,"version",0,"coordenadaVertical",6,"coordenadaHorizontal",0,"orientacion","VERTICAL")).statusCode());
        post("/api/sala",Map.of("vista",a));
        assertEquals(299,post("/api/preparacion/celda",Map.of("vista",a,"nombre",nombre,"version",3,"coordenadaVertical",6,"coordenadaHorizontal",0,"orientacion","VERTICAL")).statusCode());
        assertEquals(3,fachada.consultarBatalla(nombre).getVersion());
    }

    @Test void reconectarConservaEntradaYSecuenciaYVolverANuevaCambiaEntrada() throws Exception {
        String vista=abrir();
        JsonNode nueva=contenido(post("/api/nueva",Map.of("vista",vista)).body());
        long entrada=nueva.path("entrada").asLong(), secuencia=nueva.path("secuencia").asLong();
        assertEquals(vista,nueva.path("vista").asText());
        assertEquals(vista,contenido(post("/api/recuperar",Map.of("vista",vista)).body()).asText());
        try(Flujo f=new Flujo(vista)) {
            JsonNode resincronizada=f.siguiente();
            assertEquals(vista,resincronizada.path("vista").asText());
            assertEquals(entrada,resincronizada.path("entrada").asLong());
            assertTrue(resincronizada.path("secuencia").asLong()>secuencia);
            comando(resincronizada,"nueva");
        }
        post("/api/sala",Map.of("vista",vista));
        JsonNode reentrada=contenido(post("/api/nueva",Map.of("vista",vista)).body());
        assertTrue(reentrada.path("entrada").asLong()>entrada);
    }

    @Test void recuperaAperturaSinIdentificadorSinEjecutarJugadas() throws Exception {
        int total=fachada.consultarSala().getTotal();
        var respuesta=post("/api/recuperar",Map.of("vista","","nombre",""));
        assertEquals(200,respuesta.statusCode());
        String vista=contenido(respuesta.body()).asText(); vistas.add(vista);
        try(Flujo flujo=new Flujo(vista)) { comando(flujo.siguiente(),"sala"); }
        assertEquals(total,fachada.consultarSala().getTotal());
        assertEquals(vista,contenido(post("/api/recuperar",Map.of("vista",vista)).body()).asText());
    }

    @Test void seleccionDeEntradaAnteriorNoSobrescribeLaEleccionAlReingresar() throws Exception {
        String vista=abrir(), nombre=crear(vista);
        JsonNode primera=contenido(post("/api/entrar",Map.of("vista",vista,"nombre",nombre)).body());
        long entradaAnterior=primera.path("entrada").asLong();
        post("/api/preparacion/orientacion",Map.of("vista",vista,"nombre",nombre,"orientacion","VERTICAL","seleccionOrientacion",9,"entrada",entradaAnterior));
        post("/api/sala",Map.of("vista",vista));
        JsonNode actual=contenido(post("/api/entrar",Map.of("vista",vista,"nombre",nombre)).body());
        long entradaActual=actual.path("entrada").asLong();
        assertTrue(entradaActual>entradaAnterior);
        assertEquals(200,post("/api/preparacion/orientacion",Map.of("vista",vista,"nombre",nombre,"orientacion","HORIZONTAL","seleccionOrientacion",1,"entrada",entradaActual)).statusCode());
        assertEquals(299,post("/api/preparacion/orientacion",Map.of("vista",vista,"nombre",nombre,"orientacion","VERTICAL","seleccionOrientacion",9,"entrada",entradaAnterior)).statusCode());
        try(Flujo f=new Flujo(vista)) {
            JsonNode estado=f.siguiente();
            assertEquals("HORIZONTAL",comando(estado,"batalla").path("orientacion").asText());
            assertEquals(1,estado.path("seleccionOrientacion").asLong());
        }
        assertEquals(0,fachada.consultarBatalla(nombre).getVersion());
    }
}
