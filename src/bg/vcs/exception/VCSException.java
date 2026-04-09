package bg.vcs.exception;

/**
 * Базово изключение за грешки в логиката на системата за контрол на версиите.
 */
public class VCSException extends Exception {
    public VCSException(String message) {
        super("Error: " + message);
    }
}