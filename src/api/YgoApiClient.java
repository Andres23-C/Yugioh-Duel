package api;

import model.Card;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Consulta la API de YGOProDeck y devuelve cartas Monster.
 * No sabe nada de la interfaz: si algo falla, lanza una excepción
 * y la ventana decide cómo mostrar el mensaje.
 */
public class YgoApiClient {

    private static final String URL_ALEATORIA = "https://db.ygoprodeck.com/api/v7/randomcard.php";
    private static final int MAX_INTENTOS = 10;

    // La API redirige la petición, y el HttpClient de Java no sigue
    // redirecciones por defecto, por eso se activa aquí.
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Devuelve UNA carta Monster (vuelve a pedir si sale Spell o Trap)
    public Card obtenerCartaMonster() throws Exception {
        for (int i = 0; i < MAX_INTENTOS; i++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_ALEATORIA))
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new Exception("No se pudo cargar la carta");
            }

            // La carta viene dentro de la lista "data"
            JSONObject json = new JSONObject(response.body());
            JSONObject carta = json.getJSONArray("data").getJSONObject(0);

            // Solo sirven las cartas de tipo Monster
            String tipo = carta.optString("type", "");
            if (!tipo.contains("Monster")) {
                continue;
            }

            String nombre = carta.getString("name");
            // optInt: algunas cartas (por ejemplo las Link) no traen DEF
            int atk = carta.optInt("atk", 0);
            int def = carta.optInt("def", 0);

            String imagenUrl = "";
            JSONArray imagenes = carta.optJSONArray("card_images");
            if (imagenes != null && imagenes.length() > 0) {
                imagenUrl = imagenes.getJSONObject(0).optString("image_url", "");
            }

            return new Card(nombre, atk, def, imagenUrl);
        }
        throw new Exception("No se pudo cargar una carta Monster");
    }

    // Devuelve el mazo: "cantidad" cartas Monster
    public List<Card> obtenerMazo(int cantidad) throws Exception {
        List<Card> mazo = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            mazo.add(obtenerCartaMonster());
        }
        return mazo;
    }
}
