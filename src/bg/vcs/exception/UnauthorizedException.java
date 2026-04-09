
package bg.vcs.exception;

import bg.vcs.model.Role;



public class UnauthorizedException extends Exception {
    public UnauthorizedException(String message) {
        super(message);
    }
}
