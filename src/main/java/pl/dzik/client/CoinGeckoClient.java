package pl.dzik.client;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pl.dzik.config.AppConfig;
import pl.dzik.dto.CryptoDto;
import pl.dzik.dto.MarketDataDto;
import pl.dzik.exception.ApiException;

/**
 * Klient HTTP odpowiedzialny za komunikację z API CoinGecko.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class CoinGeckoClient {
    private static final Logger logger = LogManager.getLogger(CoinGeckoClient.class);
    private final HttpClient client;
    private final Gson gson;

    /**
     * Konstruktor tworzący zależności i konfigurujący klienta HTTP.
     */
    public CoinGeckoClient() {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.gson = new Gson();
    }

    /**
     * Konstruktor tworzący zależności i konfigurujący klienta HTTP,
     * wraz ze wstrzykniętymi zależnościami, pozwalającymi na mock w testach
     *
     * @param client klient HTTP
     * @param gson   parser JSON
     */
    public CoinGeckoClient(HttpClient client, Gson gson) {
        this.client = client;
        this.gson = gson;
    }

    /**
     * Pobiera listę TOP 250 kryptowalut posortowaną według kapitalizacji rynkowej
     *
     * @return lista obiektów {@link CryptoDto} z podstawowymi informacjami o kryptowalucie
     * @throws ApiException gdy wystąpi błąd sieci lub przekroczono limit zapytań
     */
    public List<CryptoDto> fetchTop250Cryptos() throws ApiException {
        logger.info("Wysyłanie zapytania do API o listę początkową kryptowalut (TOP 250)...");
        String baseUrl = AppConfig.get("api.url", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd");
        String finalUrl = baseUrl + "&order=market_cap_desc&per_page=250&page=1&sparkline=false";
        try {
            HttpResponse<String> response = sendGetRequest(finalUrl);
            validateSuccessStatus(response.statusCode(), "pobieranie listy kryptowalut");
            try{
                return Arrays.asList(gson.fromJson(response.body(), CryptoDto[].class));
            } catch (JsonSyntaxException | IllegalStateException e){
                logger.error("Błędny format JSON z API", e);
                throw new ApiException("Błędny format JSON z API", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Przerwano połączenie sieciowe z API CoinGecko.", e);
            throw new ApiException("Przerwano pobieranie listy kryptowalut z API.", e);
        } catch (IOException e) {
            logger.error("Krytyczny błąd podczas nawiązywania połączenie sieciowego z API.", e);
            throw new ApiException("Brak połączenia z internetem lub serwer API jest niedostępny.", e);
        }
    }

    /**
     * Pobiera aktualne dane rynkowe dla podanych identyfikatorów kryptowalut.
     *
     * @param coingeckoIds lista identyfikatorów kryptowalut
     * @return lista aktualnych danych rynkowych dla podanych kryptowalut
     * @throws ApiException gdy wystąpi błąd sieci lub przekroczono limit zapytań
     */
    public List<MarketDataDto> fetchMarketDataForIds(List<String> coingeckoIds) throws ApiException {
        if (coingeckoIds == null || coingeckoIds.isEmpty()) {
            return new ArrayList<>();
        }
        String ids = String.join(",", coingeckoIds);
        String baseUrl = AppConfig.get("api.url", "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd");
        String finalUrl = baseUrl + "&ids=" + ids + "&sparkline=false";
        logger.info("Pobieranie aktualnych kursów rynkowych z API dla: [{}]", ids);
        try {
            HttpResponse<String> response = sendGetRequest(finalUrl);
            validateSuccessStatus(response.statusCode(), "pobieranie notowań rynkowych");
            try{
                return Arrays.asList(gson.fromJson(response.body(), MarketDataDto[].class));
            } catch (JsonSyntaxException | IllegalStateException e){
                logger.error("Błędny format JSON z API", e);
                throw new ApiException("Błędny format JSON z API", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Przerwano odświeżanie kursów rynkowych z API.", e);
            throw new ApiException("Przerwano pobieranie notowań rynkowych z API.", e);
        } catch (IOException e) {
            logger.error("Błąd połączenia podczas odświeżania kursów rynkowych.", e);
            throw new ApiException("Nie udało się zaktualizować kursów kryptowalut z powodu błędu sieci.", e);
        }
    }

    /**
     * Wysyła zapytanie HTTP GET do wskazanego adresu i zwraca odpowiedź serwera
     *
     * @param url adres, do którego wysyłane jest żądanie GET
     * @return odpowiedz HTTP
     * @throws IOException          gdy błąd podczas komunikacji z serwerem
     * @throws InterruptedException jeśli wątek zostanie przerwany podczas oczekiwania na odpowiedź
     */
    private HttpResponse<String> sendGetRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Waliduje poprawność kodu odpowiedzi HTTP zwróconego przez API.
     *
     * @param statusCode kod odpowiedzi HTTP
     * @param operation  opis operacji wykonywanej na API
     * @throws ApiException gdy serwer zwróci kod błędu
     */
    private void validateSuccessStatus(int statusCode, String operation) throws ApiException {
        if (statusCode == 429) {
            logger.warn("Wykryto przekroczenie limitu zapytań HTTP 429 podczas {}.", operation);
            throw new ApiException("Przekroczono limit żądań do API (Błąd 429)");
        }
        if (statusCode != 200) {
            logger.error("Server CoinGecko zwrócił niepoprawny kod statusu {} podczas {}.", statusCode, operation);
            throw new ApiException("Server odpowiedział błędem o kodzie: " + statusCode);
        }
    }
}
