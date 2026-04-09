package bg.vcs.exception;


public class VCSException extends Exception {
    public VCSException(String message) {
        super("Error: " + message);
    }
}