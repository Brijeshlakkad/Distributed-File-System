package rmi;


/**
 * Catches the exception occurred in the <code>Thread</code> execution.
 */
public class ListenErrorHandler implements Thread.UncaughtExceptionHandler {
    private Skeleton<?> d_skeleton;

    /**
     * Parameterized constructor to set the skeleton for this instance.
     *
     * @param p_skeleton Skeleton for which this handler is being attached.
     */
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