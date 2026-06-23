package pl.dzik.model;

/**
 * Reprezentuje wpis na liście obserwowanych kryptowalut.
 *
 * @param id
 * @param cryptoId
 * @param isActive
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public record Watchlist(
        int id,
        int cryptoId,
        boolean isActive
) {
}
