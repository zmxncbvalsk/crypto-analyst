package pl.dzik.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Klasa DTO reprezentująca podstawowe informacje o kryptowalucie zwracane przez API.
 *
 * @param id            unikalny identyfikator
 * @param symbol        symbol kryptowaluty
 * @param name          nazwa kryptowaluty
 * @param image         ikona kryptowaluty
 * @param marketCapRank pozycja w rankingu kryptowaluty
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public record CryptoDto(
        String id,
        String symbol,
        String name,
        String image,
        @SerializedName("market_cap_rank")
        int marketCapRank
) {
}
