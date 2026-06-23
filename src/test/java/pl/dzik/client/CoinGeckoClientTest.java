package pl.dzik.client;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dzik.client.CoinGeckoClient;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CoinGeckoClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private final Gson gson = new Gson();
    private CoinGeckoClient client;

    @BeforeEach
    void setUp() {
        client = new CoinGeckoClient(httpClient, gson);
    }

    @Test
    @DisplayName("fetchTop250Cryptos zwraca sparsowaną listę przy odpowiedzi 200")
    void fetchTop250Cryptos_Response200_ReturnsList() throws Exception {
        String json = """
                [{"id":"bitcoin","symbol":"btc","name":"Bitcoin","image":"img.png","market_cap_rank":1}]
                """;
        stubHttpResponse(200, json);
        var result = client.fetchTop250Cryptos();
        assertEquals(1, result.size());
        assertEquals("bitcoin", result.getFirst().id());
    }

    @Test
    @DisplayName("fetchTop250Cryptos rzuca ApiException przy statusie 429")
    void fetchTop250Cryptos_Status429_ThrowsApiException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> httpResponse);
        when(httpResponse.statusCode()).thenReturn(429);
        ApiException ex = assertThrows(ApiException.class, () -> client.fetchTop250Cryptos());
        assertTrue(ex.getMessage().contains("429"));
    }

    @Test
    @DisplayName("fetchTop250Cryptos rzuca ApiException przy niepoprawnym JSON")
    void fetchTop250Cryptos_InvalidJson_ThrowsApiException() throws Exception {
        stubHttpResponse(200, "not-valid-json");

        assertThrows(ApiException.class, () -> client.fetchTop250Cryptos());
    }

    @Test
    @DisplayName("fetchTop250Cryptos rzuca ApiException przy błędzie IO")
    void fetchTop250Cryptos_IoException_ThrowsApiException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenThrow(new IOException("Brak sieci"));
        assertThrows(ApiException.class, () -> client.fetchTop250Cryptos());
    }

    @Test
    @DisplayName("fetchMarketDataForIds zwraca pustą listę dla null lub pustego wejścia")
    void fetchMarketDataForIds_EmptyInput_ReturnsEmptyList() throws Exception {
        assertTrue(client.fetchMarketDataForIds(null).isEmpty());
        assertTrue(client.fetchMarketDataForIds(List.of()).isEmpty());
    }

    @Test
    @DisplayName("fetchMarketDataForIds zwraca dane przy poprawnej odpowiedzi API")
    void fetchMarketDataForIds_Response200_ReturnsData() throws Exception {
        String json = """
                [{
                    "id": "bitcoin",
                    "symbol": "btc",
                    "name": "Bitcoin",
                    "image": "https://assets.coingecko.com/btc.png",
                    "current_price": 64995.50,
                    "total_volume": 28000000000,
                    "market_cap": 1280000000000,
                    "price_change_percentage_24h": -2.35
                }]
                """;
        stubHttpResponse(200, json);
        List<MarketDataDto> result = client.fetchMarketDataForIds(List.of("bitcoin"));
        assertEquals(1, result.size());
        assertEquals("bitcoin", result.getFirst().id());
        assertEquals(new BigDecimal("64995.50"), result.getFirst().currentPrice());
    }

    @Test
    @DisplayName("fetchMarketDataForIds rzuca ApiException przy niepoprawnym JSON")
    void fetchMarketDataForIds_InvalidJson_ThrowsApiException() throws Exception {
        stubHttpResponse(200, "{broken");

        assertThrows(ApiException.class, () -> client.fetchMarketDataForIds(List.of("bitcoin")));
    }

    private void stubHttpResponse(int statusCode, String body) throws Exception {
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(invocation -> httpResponse);
        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(body);
    }
}