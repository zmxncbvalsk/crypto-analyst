package pl.dzik.exception;

/**
 * Główny, bazowy wyjątek aplikacji.
 *
 * @author Bartłomiej Dzik
 * @version 1.0
 */
public class ApplicationException extends RuntimeException {
    /**
     * Tworzy wyjątek z komunikatem opisującym problem.
     *
     * @param message opis błędu widoczny dla użytkownika lub w logach
     */
    public ApplicationException(String message) {
        super(message);
    }

    /**
     * Tworzy wyjątek z komunikatem i przyczyną pierwotną.
     *
     * @param message opis błędu
     * @param cause   pierwotny wyjątek
     */
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
