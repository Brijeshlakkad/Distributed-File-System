package rmi;


/**
 * Catches the exception occurred in the Thread execution.
 */
public class ListenErrorHandler implements Thread.UncaughtExceptionHandler {
    private Skeleton<?> d_skeleton;

    public ListenErrorHandler(Skeleton<?> p_skeleton) {
        d_skeleton = p_skeleton;
    }

    /**
     * {@inheritDoc}
     */
    public void uncaughtException(Thread t, Throwable e) {
        d_skeleton.listen_error(new Exception(e));

        // TODO Skeleton can be start again.
    }
}