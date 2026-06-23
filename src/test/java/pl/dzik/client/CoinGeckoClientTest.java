package pl.dzik.client;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pl.dzik.dto.CryptoDto;
import pl.dzik.dto.MarketDataDto;
import pl.dzik.exception.ApiException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoinGeckoClientTest {

    @Mock
    private HttpClient httpClient;

    private CoinGeckoClient client;

    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        client = new CoinGeckoClient(httpClient, gson);
    }

    private HttpResponse<String> response(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }

    @SuppressWarnings("unchecked")
    private void stubSend(HttpResponse<String> response) throws Exception {
        doReturn(response)
                .when(httpClient)
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @SuppressWarnings("unchecked")
    private void stubSendException(IOException exception) throws Exception {
        doThrow(exception)
                .when(httpClient)
                .send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("TOP 250 - poprawna odpowiedź")
    void fetchTop250_ok() throws Exception {
        String json = """
            [
              {"id":"bitcoin","symbol":"btc","name":"Bitcoin","image":"img.png","market_cap_rank":1}
            ]
        """;

        stubSend(response(200, json));

        List<CryptoDto> result = client.fetchTop250Cryptos();

        assertEquals(1, result.size());
        assertEquals("bitcoin", result.getFirst().id());
        assertEquals("btc", result.getFirst().symbol());
        assertEquals("Bitcoin", result.getFirst().name());
    }

    @Test
    @DisplayName("TOP 250 - 429 → ApiException")
    void fetchTop250_429() throws Exception {
        stubSend(response(429, "rate limit"));

        ApiException ex = assertThrows(ApiException.class, () -> client.fetchTop250Cryptos());

        assertTrue(ex.getMessage().contains("429"));
    }

    @Test
    @DisplayName("TOP 250 - invalid JSON → ApiException")
    void fetchTop250_invalidJson() throws Exception {
        stubSend(response(200, "not-json"));

        assertThrows(ApiException.class, () -> client.fetchTop250Cryptos());
    }

    @Test
    @DisplayName("TOP 250 - IOException → ApiException")
    void fetchTop250_ioException() throws Exception {
        stubSendException(new IOException("Brak sieci"));

        assertThrows(ApiException.class, () -> client.fetchTop250Cryptos());
    }

    @Test
    @DisplayName("Market data - pusty input")
    void marketData_empty() throws Exception {
        assertTrue(client.fetchMarketDataForIds(null).isEmpty());
        assertTrue(client.fetchMarketDataForIds(List.of()).isEmpty());
    }

    @Test
    @DisplayName("Market data - poprawna odpowiedź")
    void marketData_ok() throws Exception {
        String json = """
            [
              {
                "id": "bitcoin",
                "symbol": "btc",
                "name": "Bitcoin",
                "image": "https://assets.coingecko.com/btc.png",
                "current_price": 64995.50,
                "total_volume": 28000000000,
                "market_cap": 1280000000000,
                "price_change_percentage_24h": -2.35
              }
            ]
        """;

        stubSend(response(200, json));

        List<MarketDataDto> result = client.fetchMarketDataForIds(List.of("bitcoin"));

        assertEquals(1, result.size());
        assertEquals("bitcoin", result.getFirst().id());
        assertEquals(new BigDecimal("64995.50"), result.getFirst().currentPrice());
    }

    @Test
    @DisplayName("Market data - invalid JSON → ApiException")
    void marketData_invalidJson() throws Exception {
        stubSend(response(200, "{broken"));

        assertThrows(ApiException.class, () -> client.fetchMarketDataForIds(List.of("bitcoin")));
    }
}