package master;

/**
 * Unchecked exception thrown from within recursive algorithm methods when the
 * worker thread has been interrupted due to a timeout. It propagates up the
 * entire call stack, unwinding all recursive frames immediately.
 */
public class AlgorithmTimeoutException extends RuntimeException {
    public AlgorithmTimeoutException() {
        super("Algorithm timed out");
    }
}
