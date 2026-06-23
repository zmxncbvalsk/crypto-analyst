package pl.dzik.enums;

/**
 * Określa globalny stan działania aplikacji wyświetlany w interfejsie użytkownika.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public enum SystemStatus {
    /**
     * Aplikacja uruchamia komponenty.
     */
    STARTING,
    /**
     * Aplikacja działa prawidłowo.
     */
    RUNNING,
    /**
     * Aplikacja działa z ograniczeniami (np. błąd API).
     */
    DEGRADED,
    /**
     * Wystąpił krytyczny błąd wymagający uwagi użytkownika.
     */
    ERROR
}