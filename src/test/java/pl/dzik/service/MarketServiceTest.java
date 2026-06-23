package pl.dzik.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.dzik.client.CoinGeckoClient;
import pl.dzik.dao.CryptoDao;
import pl.dzik.dao.MarketDataDao;
import pl.dzik.dao.WatchlistDao;
import pl.dzik.dto.MarketDataDto;
import pl.dzik.exception.ApiException;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Crypto;
import pl.dzik.model.Watchlist;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MarketServiceTest {
    @Mock
    private CoinGeckoClient client;
    @Mock
    private CryptoDao cryptoDao;
    @Mock
    private WatchlistDao watchlistDao;
    @Mock
    private MarketDataDao marketDataDao;
    @Mock
    private AlertService alertService;

    private MarketService marketService;

    @BeforeEach
    void setUp(){
        marketService = new MarketService(client, cryptoDao, watchlistDao, marketDataDao, alertService);
    }

    @Test
    @DisplayName("refreshMarketData zapisuje dane i sprawdza alerty dla aktywnych pozycji")
    void refreshMarketData_ActiveWatchlist_processMarketData() throws Exception {
        Watchlist item = new Watchlist(1, 10, true);
        Crypto crypto = new Crypto(10, "bitcoin", "btc", "Bitcoin", "btc.png", 1);
        MarketDataDto dto = new MarketDataDto(
                "bitcoin", "btc", "Bitcoin", "btc.png",
                new BigDecimal("65000"), new BigDecimal("1000"),
                new BigDecimal("1000000"), new BigDecimal("1.5"));
        when(watchlistDao.findActiveCoinGeckoIds()).thenReturn(List.of("bitcoin"));
        when(cryptoDao.findByCoinGeckoId("bitcoin")).thenReturn(crypto);
        when(client.fetchMarketDataForIds(List.of("bitcoin"))).thenReturn(List.of(dto));
        marketService.refreshMarketData();
        verify(marketDataDao).save(any());
        verify(alertService).checkAndTriggerAlerts(eq(crypto), eq(new BigDecimal("65000")));
    }

    @Test
    @DisplayName("searchCrypto deleguje zapytanie do DAO")
    void searchCrypto_ReturnResultsFromDao() throws DatabaseException {
        Crypto crypto = new Crypto(1, "bitcoin", "btc", "Bitcoin", "btc.png", 1);
        when(cryptoDao.searchByPhrase("bit")).thenReturn(List.of(crypto));
        List<Crypto> results = marketService.searchCrypto("bit");
        assertEquals(1, results.size());
        assertEquals("bitcoin", results.getFirst().coingeckoId());
    }

    @Test
    @DisplayName("refreshMarketData pomija rekordy API z null polami liczbowymi")
    void refreshMarketData_NullFieldApi_SkipSave() throws Exception {
        Watchlist item = new Watchlist(1, 10, true);
        Crypto crypto = new Crypto(10, "bitcoin", "btc", "Bitcoin", "btc.png", 1);
        MarketDataDto dtoWithNullPrice = new MarketDataDto(
                "bitcoin", "btc", "Bitcoin", "btc.png",
                null, new BigDecimal("1000"), new BigDecimal("1000000"), new BigDecimal("1.5"));
        when(watchlistDao.findActiveCoinGeckoIds()).thenReturn(List.of("bitcoin"));
        when(client.fetchMarketDataForIds(List.of("bitcoin"))).thenReturn(List.of(dtoWithNullPrice));
        marketService.refreshMarketData();
        verify(marketDataDao, never()).save(any());
        verify(alertService, never()).checkAndTriggerAlerts(any(), any());
    }

    @Test
    @DisplayName("addToWatchlist nie zapisuje, gdy kryptowaluta nie istnieje w słowniku")
    void addToWatchlist_BrakKrypto_NieDodaje() throws DatabaseException {
        when(cryptoDao.findByCoinGeckoId("unknown")).thenReturn(null);
        marketService.addToWatchlist("unknown");
        verify(watchlistDao, never()).add(anyInt());
    }

    @Test
    @DisplayName("Gdy synchronizacja słownika napotka błąd API, metoda loguje i nie przerywa wątku")
    void synchronizeCryptoDictionary_ApiError_HandlesExceptionWithoutCrash() throws ApiException {
        when(client.fetchTop250Cryptos()).thenThrow(new ApiException("Błąd połączenia sieciowego z serwerem CoinGecko"));
        assertDoesNotThrow(() -> marketService.synchronizeCryptoDictionary());
    }

    @Test
    @DisplayName("Gdy synchronizacja napotka błąd bazy, metoda loguje i nie przerywa wątku")
    void synchronizeCryptoDictionary_DatabaseError_HandlesExceptionWithoutCrash() throws Exception {
        when(client.fetchTop250Cryptos()).thenReturn(java.util.List.of(
                new pl.dzik.dto.CryptoDto("bitcoin", "btc", "Bitcoin", "btc.png", 1)));
        org.mockito.Mockito.doThrow(new DatabaseException("Błąd zapisu"))
                .when(cryptoDao).saveOrUpdate(org.mockito.ArgumentMatchers.any());
        assertDoesNotThrow(() -> marketService.synchronizeCryptoDictionary());
    }
}
