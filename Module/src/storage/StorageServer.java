package storage;

import common.Path;
import naming.Registration;
import rmi.RMIException;
import rmi.Skeleton;
import rmi.Stub;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;

/**
 * Storage server.
 *
 * <p>
 * Storage servers respond to client file access requests. The files accessible through a storage server are those
 * accessible under a given directory of the local filesystem.
 */
public class StorageServer implements Storage, Command, Remote {
    private File d_root;
    private Skeleton<Storage> d_storageSkeleton;
    private Skeleton<Command> d_commandSkeleton;
    private boolean alive = false;

    /**
     * Creates a storage server, given a directory on the local filesystem.
     *
     * @param root Directory on the local filesystem. The contents of this directory will be accessible through the
     *             storage server.
     * @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root) {
        if (root == null) {
            throw new NullPointerException("Root is null");
        }
        d_root = root;
        d_storageSkeleton = new Skeleton<>(Storage.class, this);
        d_commandSkeleton = new Skeleton<>(Command.class, this);
        alive = true;
    }

    /**
     * Starts the storage server and registers it with the given naming server.
     *
     * @param hostname      The externally-routable hostname of the local host on which the storage server is running.
     *                      This is used to ensure that the stub which is provided to the naming server by the
     *                      <code>start</code> method carries the externally visible hostname or address of this
     *                      storage server.
     * @param naming_server Remote interface for the naming server with which the storage server is to register.
     * @throws UnknownHostException  If a stub cannot be created for the storage server because a valid address has not
     *                               been assigned.
     * @throws FileNotFoundException If the directory with which the server was created does not exist or is in fact a
     *                               file.
     * @throws RMIException          If the storage server cannot be started, or if it cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
            throws RMIException, UnknownHostException, FileNotFoundException {
        if (!alive)
            throw new RMIException("Failed to start! Calling again may give the same result");
        if (!d_root.exists() || d_root.isFile()) {
            throw new FileNotFoundException("Root either doesn't exist or is a file.");
        }

        // Validate the hostname.
        InetAddress.getByName(hostname);

        try {
            d_storageSkeleton.start();
            d_commandSkeleton.start();
            Storage l_storageStub = Stub.create(Storage.class, d_storageSkeleton, hostname);
            Command l_commandStub = Stub.create(Command.class, d_commandSkeleton, hostname);
            Path[] duplicates = naming_server.register(l_storageStub, l_commandStub, Path.list(d_root));

            // TODO PRUNE
        } catch (RMIException p_rmiException) {
            alive = false;
            throw new RMIException("Couldn't start naming server", p_rmiException);
        }
    }

    /**
     * Stops the storage server.
     *
     * <p>
     * The server should not be restarted.
     */
    public void stop() {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Called when the storage server has shut down.
     *
     * @param cause The cause for the shutdown, if any, or <code>null</code> if the server was shut down by the user's
     *              request.
     */
    protected void stopped(Throwable cause) {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
            throws FileNotFoundException, IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
            throws FileNotFoundException, IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized boolean delete(Path path) {
        throw new UnsupportedOperationException("not implemented");
    }
}
