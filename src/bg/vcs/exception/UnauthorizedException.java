
package bg.vcs.exception;

import bg.vcs.model.Role;

/**
 * Изключение, което се хвърля при опит за действие без нужните права.
 */
public class UnauthorizedException extends Exception {
    public UnauthorizedException(String message) {
        super(message);
    }
}
