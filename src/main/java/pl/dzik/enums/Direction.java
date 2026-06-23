package pl.dzik.enums;

/**
 * Określa kierunek progu cenowego alertu.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public enum Direction {
    /**
     * Alert wyzwalany, gdy cena wzrośnie do wartości docelowej lub powyżej.
     */
    ABOVE,
    /**
     * Alert wyzwalany, gdy cena spadnie do wartości docelowej lub poniżej.
     */
    BELOW
}