package pl.dzik.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.dzik.client.CoinGeckoClient;
import pl.dzik.dao.CryptoDao;
import pl.dzik.dao.MarketDataDao;
import pl.dzik.dao.WatchlistDao;
import pl.dzik.dto.CryptoDto;
import pl.dzik.dto.MarketDataDto;
import pl.dzik.exception.ApiException;
import pl.dzik.exception.DatabaseException;
import pl.dzik.model.Crypto;
import pl.dzik.model.MarketData;
import pl.dzik.model.Watchlist;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Główny serwis koordynujący pobieranie danych z sieci, aktualizację bazy danych
 * oraz wyzwalanie procesów analizy rynkowej i alertów.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class MarketService {
    private static final Logger logger = LogManager.getLogger(MarketService.class);
    private final CoinGeckoClient client;
    private final CryptoDao cryptoDao;
    private final WatchlistDao watchlistDao;
    private final MarketDataDao marketDataDao;
    private final AlertService alertService;

    /**
     * Tworzy serwis z domyślnymi zależnościami.
     */
    public MarketService() {
        this.client = new CoinGeckoClient();
        this.cryptoDao = new CryptoDao();
        this.watchlistDao = new WatchlistDao();
        this.marketDataDao = new MarketDataDao();
        this.alertService = new AlertService();
    }

    /**
     * Konstruktor tworzący zależności i konfigurujący serwis marketowy,
     * wraz ze wstrzykniętymi zależnościami, pozwalającymi na mock w testach
     *
     * @param client        klient HTTP CoinGecko
     * @param cryptoDao     DAO kryptowalut
     * @param watchlistDao  DAO listy obserwowanych
     * @param marketDataDao DAO danych rynkowych
     * @param alertService  serwis alertów cenowych
     */
    public MarketService(
            CoinGeckoClient client,
            CryptoDao cryptoDao,
            WatchlistDao watchlistDao,
            MarketDataDao marketDataDao,
            AlertService alertService
    ) {
        this.client = client;
        this.cryptoDao = cryptoDao;
        this.watchlistDao = watchlistDao;
        this.marketDataDao = marketDataDao;
        this.alertService = alertService;
    }

    /**
     * Pobiera z API listę TOP 250 kryptowalut i synchronizuje lokalny słownik w bazie danych.
     */
    public void synchronizeCryptoDictionary() {
        logger.info("Uruchamianie synchronizacji słownika kryptowalut...");
        try {
            List<CryptoDto> cryptoDto = client.fetchTop250Cryptos();
            for (CryptoDto dto : cryptoDto) {
                Crypto crypto = new Crypto(0, dto.id(), dto.symbol(), dto.name(), dto.image(), dto.marketCapRank());
                cryptoDao.saveOrUpdate(crypto);
            }
            logger.info("Synchronizacja słownika zakończona sukcesem. Zaktualizowano {} pozycji.", cryptoDto.size());
        } catch (ApiException e) {
            logger.error("Nie udało się zsynchronizować słownika kryptowalut z API: {}", e.getMessage());
        } catch (DatabaseException e) {
            logger.error("Nie udało się zapisać słownika kryptowalut w bazie: {}", e.getMessage());
        }
    }

    /**
     * Pobiera aktualne dane rynkowe dla aktywnych pozycji.
     *
     * @throws ApiException      gdy wystąpi błąd komunikacji z API
     * @throws DatabaseException gdy wystąpi błąd zapisu lub odczytu w bazie
     */
    public void refreshMarketData() throws ApiException, DatabaseException {
        logger.info("Rozpoczęcie procedury odświeżania notowań rynkowych...");
        List<String> ids = watchlistDao.findActiveCoinGeckoIds();
        if (ids.isEmpty()) {
            logger.info("Brak aktywnych kryptowalut do monitorowania.");
            return;
        }
        List<MarketDataDto> marketDataDto = client.fetchMarketDataForIds(ids);
        processMarketData(marketDataDto);
        logger.info("Zakończono procedurę odświeżania notowań rynkowych");
    }

    /**
     * Dodaje kryptowalutę do listy obserwowanych na podstawie identyfikatora CoinGecko.
     *
     * @param coingeckoId identyfikator CoinGecko (np. {@code bitcoin})
     * @throws DatabaseException gdy operacja na bazie nie powiedzie się
     */
    public void addToWatchlist(String coingeckoId) throws DatabaseException {
        Crypto crypto = cryptoDao.findByCoinGeckoId(coingeckoId);
        if (crypto == null) {
            return;
        }
        watchlistDao.add(crypto.id());
        logger.debug("Dodano {} do listy obserwowanych.", crypto.name());
    }

    /**
     * Usuwa kryptowalutę z listy obserwowanych na podstawie identyfikatora CoinGecko.
     *
     * @param coingeckoId identyfikator CoinGecko
     * @throws DatabaseException gdy operacja na bazie nie powiedzie się
     */
    public void removeFromWatchlist(String coingeckoId) throws DatabaseException {
        Crypto crypto = cryptoDao.findByCoinGeckoId(coingeckoId);
        if (crypto == null) {
            return;
        }
        watchlistDao.removeByCryptoId(crypto.id());
        logger.debug("Usunięto {} z listy obserwowanych.", crypto.name());
    }

    /**
     * Wyszukuje kryptowaluty pasujące do podanej frazy.
     *
     * @param phrase fraza wyszukiwania (nazwa lub symbol)
     * @return lista dopasowanych kryptowalut
     * @throws DatabaseException gdy zapytanie do bazy nie powiedzie się
     */
    public List<Crypto> searchCrypto(String phrase) throws DatabaseException {
        return cryptoDao.searchByPhrase(phrase);
    }

    /**
     * Przetwarza i zapisuje dane rynkowe do bazy danych oraz przekazuje weryfikacje alertów cenowych.
     *
     * @param marketDataDto lista obiektów DTO świeżych danych rynkowych
     * @throws DatabaseException gdy zapytanie do bazy nie powiedzie się
     */
    private void processMarketData(List<MarketDataDto> marketDataDto) throws DatabaseException {
        for (MarketDataDto dto : marketDataDto) {
            if (!isComplete(dto)) {
                continue;
            }
            Crypto crypto = cryptoDao.findByCoinGeckoId(dto.id());
            if (crypto == null) {
                continue;
            }
            MarketData marketData = new MarketData(
                    0,
                    crypto.id(),
                    dto.currentPrice(),
                    dto.totalVolume(),
                    dto.marketCap(),
                    dto.change24h(),
                    LocalDateTime.now()
            );
            marketDataDao.save(marketData);
            logger.debug("Zarchiwizowano próbkę cenową dla: {} (Cena: {})", crypto.name(), dto.currentPrice());
            alertService.checkAndTriggerAlerts(crypto, dto.currentPrice());
        }
    }

    /**
     * Sprawdza poprawność odpowiedzi zapytania API.
     *
     * @param dto obiekt DTO do sprawdzenia
     * @return {@code true}, jeśli obiekt DTO zawiera wszystkie wymagane dane rynkowe
     */
    private boolean isComplete(MarketDataDto dto) {
        if (dto == null || dto.id() == null || dto.id().isBlank()) {
            return false;
        }
        return dto.currentPrice() != null &&
                dto.totalVolume() != null &&
                dto.marketCap() != null &&
                dto.change24h() != null;
    }
}
