package pl.dzik.model;

/**
 * Reprezentuje kryptowalutę w lokalnym słowniku aplikacji.
 *
 * @param id            klucz główny
 * @param coingeckoId   unikalny klucz CoinGecko
 * @param symbol        symbol kryptowaluty
 * @param name          nazwa kryptowaluty
 * @param image         ikona kryptowaluty
 * @param marketCapRank ranking kapitalizacji rynkowej
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public record Crypto(
        int id,
        String coingeckoId,
        String symbol,
        String name,
        String image,
        int marketCapRank
) {
}
