package storage;

import common.Path;
import naming.Registration;
import rmi.RMIException;
import rmi.Skeleton;
import rmi.Stub;

import java.io.*;
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
    private volatile boolean alive = false;

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
        if (alive)
            throw new RMIException("Failed to start! Calling again may give the same result");
        if (!d_root.exists() || d_root.isFile()) {
            throw new FileNotFoundException("Root either doesn't exist or is a file.");
        }

        synchronized (this) {
            alive = true;
        }

        // Validate the hostname.
        InetAddress.getByName(hostname);

        try {
            d_storageSkeleton.start();
            d_commandSkeleton.start();
            Storage l_storageStub = Stub.create(Storage.class, d_storageSkeleton, hostname);
            Command l_commandStub = Stub.create(Command.class, d_commandSkeleton, hostname);
            Path[] duplicates = naming_server.register(l_storageStub, l_commandStub, Path.list(d_root));

            pruneDuplicateFiles(duplicates);
        } catch (RMIException p_rmiException) {
            synchronized (this) {
                alive = false;
            }
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
        try {
            d_storageSkeleton.stop();
            d_commandSkeleton.stop();
            stopped(null);
        } catch (Exception e) {
            stopped(e);
        } finally {
            synchronized (this) {
                alive = false;
            }
        }
    }

    /**
     * Called when the storage server has shut down.
     *
     * @param cause The cause for the shutdown, if any, or <code>null</code> if the server was shut down by the user's
     *              request.
     */
    protected void stopped(Throwable cause) {
    }

    private void pruneDuplicateFiles(Path[] duplicates) {
        for (Path l_duplicateFilePath : duplicates) {
            delete(l_duplicateFilePath);
        }
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException("Null parameter(s) found!");
        }
        File l_file = file.toFile(this.d_root);
        if (!l_file.exists() || !l_file.isFile()) {
            throw new FileNotFoundException("File not found!");
        }
        return l_file.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
            throws FileNotFoundException, IOException {
        if (file == null) {
            throw new NullPointerException("Null parameter(s) found!");
        }
        File l_file = file.toFile(this.d_root);
        if (!l_file.exists() || !l_file.isFile()) {
            throw new FileNotFoundException("File not found!");
        }
        // Instantiate array
        byte[] l_bytes = new byte[(int) l_file.length()];
        try (FileInputStream l_inputStream = new FileInputStream(l_file)) {
            // Read all bytes of File stream
            l_inputStream.read(l_bytes, (int) offset, length);
        }
        return l_bytes;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
            throws FileNotFoundException, IOException {
        if (file == null) {
            throw new NullPointerException("Null parameter(s) found!");
        }
        File l_file = file.toFile(this.d_root);
        if (!l_file.exists() || !l_file.isFile()) {
            throw new FileNotFoundException("File not found!");
        }
        long l_lengthOfFile = l_file.length() + data.length;
        try (FileOutputStream l_outputStream = new FileOutputStream(l_file)) {
            l_outputStream.write(data, (int) offset, (int) l_lengthOfFile);
        }
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file) {
        if (file == null) {
            throw new NullPointerException("Null parameter(s) found!");
        }
        File l_file = file.toFile(this.d_root);
        try {
            return l_file.createNewFile();
        } catch (IOException p_ioException) {
            return false;
        }
    }

    @Override
    public synchronized boolean delete(Path path) {
        if (path == null) {
            throw new NullPointerException("Null parameter(s) found!");
        }
        File l_fileToBeDeleted = new File(this.d_root, path.toString());
        l_fileToBeDeleted.delete();
        try {
            return deleteIfEmptyFolder(path.parent());
        } catch (IllegalArgumentException p_e) {
            return true;
        }
    }

    private synchronized boolean deleteIfEmptyFolder(Path p_path) {
        try {
            File l_fileToBeDeleted = new File(this.d_root, p_path.toString());
            if (l_fileToBeDeleted.isDirectory() && l_fileToBeDeleted.listFiles() != null
                    && l_fileToBeDeleted.listFiles().length == 0) {
                l_fileToBeDeleted.delete();
                return deleteIfEmptyFolder(p_path.parent());
            } else return !l_fileToBeDeleted.isDirectory() || l_fileToBeDeleted.listFiles() == null
                    || l_fileToBeDeleted.listFiles().length <= 0;
        } catch (IllegalArgumentException p_e) {
            return true;
        }
    }
}
