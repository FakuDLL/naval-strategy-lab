package dev.navalstrategylab;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BibliotecasWebTest {
    @LocalServerPort
    private int puerto;

    @ParameterizedTest
    @CsvSource({
        "CommandDispatcher.js,533d25c18a71955ed0a15c836e82ad135c1af19f8724c1e55bca7f3dbb2e9fbd",
        "BattleShipBoard.js,a6c586b2866147bb120fd56ecc8bd8b4c3ac81ce334c9a2cc90acfd7e7ffecea"
    })
    void servidorArrancaYEntregaBibliotecaOriginalSinModificar(String archivo, String hashOriginal)
            throws Exception {
        try (HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            HttpRequest solicitud = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + puerto + "/js/" + archivo))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<byte[]> respuesta = cliente.send(solicitud, HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, respuesta.statusCode());
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(respuesta.body()));
            assertEquals(hashOriginal, hash, "Se modificó una biblioteca protegida del curso");
        }
    }
}
