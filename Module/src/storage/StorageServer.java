package storage;

import common.Path;
import naming.Registration;
import rmi.RMIException;
import rmi.Skeleton;
import rmi.Stub;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.rmi.Remote;

/**
 * Storage server.
 *
 * <p>
 * Storage servers respond to client file access requests. The files accessible through a storage server are those
 * accessible under a given directory of the local filesystem.
 */
public class StorageServer implements Storage, Command, Remote {
    private final File d_root;
    private final Skeleton<Storage> d_storageSkeleton;
    private final Skeleton<Command> d_commandSkeleton;
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
    public synchronized long size(Path p_path) throws FileNotFoundException {
        File requiredFile = new File(this.d_root, p_path.toString());
        if (requiredFile.isDirectory() || !requiredFile.exists()) {
            throw new FileNotFoundException("File is either a directory or File not found");
        }
        // size not function like that.
        return requiredFile.length();
    }

    @Override
    public synchronized byte[] read(Path p_path, long p_offset, int p_length)
            throws FileNotFoundException, IOException {
        // Retrieve the file.
        File l_requiredFile = retrieveFileForReadOrWrite(p_path);

        // If it can be read or not.
        if (!l_requiredFile.canRead()) {
            throw new IOException("Permission Error: File can not be read!");
        }

        // If improper data provided for offset or length.
        if (p_offset < 0 || p_length < 0) {
            throw new IndexOutOfBoundsException("Invalid parameters.");
        }

        // If the file length is less than asked to read for.
        if ((p_offset + p_length) > l_requiredFile.length()) {
            throw new IndexOutOfBoundsException("Invalid the offset or length!");
        }

        /* Two ways to set offset and read the file content:
        1. RandomAccessFile
        2. FileInputStream.getChannel().position(int) method
         */
        RandomAccessFile l_randomAccessFile = new RandomAccessFile(l_requiredFile, "r");
        byte[] l_data = new byte[p_length];

        // Read the file content with in specified range.
        l_randomAccessFile.seek(p_offset);
        l_randomAccessFile.readFully(l_data, 0, p_length);
        return l_data;
    }

    @Override
    public synchronized void write(Path p_path, long p_offset, byte[] p_data)
            throws FileNotFoundException, IOException {
        if (p_data == null) {
            throw new NullPointerException("Invalid parameters.");
        }

        if (p_offset < 0) {
            throw new IndexOutOfBoundsException("Offset given negative");
        }

        // Retrieve the file.
        File l_requiredFile = retrieveFileForReadOrWrite(p_path);

        // If data can be written into it.
        if (!l_requiredFile.canWrite()) {
            throw new IOException("File is not given a write access");
        }

        RandomAccessFile l_randomAccessFile = new RandomAccessFile(l_requiredFile, "rw");

        // Write into the file setting the offset.
        l_randomAccessFile.seek(p_offset);
        l_randomAccessFile.write(p_data);
    }

    /**
     * Retrieves the file after validating the file path and returns the file before checking that file doesn't
     * represent directory and it exists.
     *
     * @param p_path Value of the file path.
     * @return Value of <code>File</code> object.
     * @throws FileNotFoundException If file represents a directory or can not be found.
     * @throws NullPointerException  If file path is null.
     */
    private File retrieveFileForReadOrWrite(Path p_path) throws FileNotFoundException, NullPointerException {
        if (p_path == null) {
            throw new NullPointerException("File path is null!");
        }
        // Fetch the file.
        File l_requiredFile = new File(this.d_root, p_path.toString());

        // If it exists or is a file or not.
        if (!l_requiredFile.exists() || l_requiredFile.isDirectory()) {
            throw new FileNotFoundException("Input either a directory or File not found!");
        }

        return l_requiredFile;
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path p_path) {
        // Check if the file is root directory.
        if (p_path.isRoot()) {
            return false;
        }
        // Fetch the file object.
        File l_requiredFile = new File(this.d_root, p_path.toString());

        // If parent directories don't exist, create them.
        File l_parentDirectory = l_requiredFile.getParentFile();
        l_parentDirectory.mkdirs();

        try {
            // Create a new file
            return l_requiredFile.createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public synchronized boolean delete(Path p_path) {
        if (p_path == null) {
            throw new NullPointerException("Null parameter(s)!");
        }
        if (p_path.isRoot()) {
            return false;
        }
        // Fetch the file object to be deleted.
        File l_fileToBeDeleted = new File(this.d_root, p_path.toString());

        boolean l_hasFileDeleted = false;
        try {
            try {
                if (l_fileToBeDeleted.isDirectory()) {
                    long deleted = deleteIfDirectory(l_fileToBeDeleted);
                    long expectedDeletionCount = (l_fileToBeDeleted.listFiles() == null || l_fileToBeDeleted.listFiles().length <= 0) ? 1 : l_fileToBeDeleted.listFiles().length + 1;
                    // if the directory doesn't exist, l_hasFileDeleted will be set to true.
                    l_hasFileDeleted = deleted == expectedDeletionCount;
                } else {
                    Files.delete(l_fileToBeDeleted.toPath());
                }
                l_hasFileDeleted = !l_fileToBeDeleted.exists();
            } catch (IOException p_ioException) {
                return false;
            }
            // Can be used a thread for this as it may take a unexpected amount of time for huge collections of directories?
            deleteIfEmptyFolder(p_path.parent());
            return l_hasFileDeleted;
        } catch (IllegalArgumentException p_e) {
            return l_hasFileDeleted;
        }
    }

    private long deleteIfDirectory(File file) {
        long deleted = 0;
        File[] list = file.listFiles();
        if (list != null && list.length > 0) {
            for (File temp : list) {
                // Recursive delete approach
                deleted += deleteIfDirectory(temp);
            }
        }

        try {
            Files.delete(file.toPath());
            return deleted + 1;
        } catch (IOException p_ioException) {
        }
        return deleted;
    }

    private synchronized void deleteIfEmptyFolder(Path p_path) {
        if (p_path.isRoot()) {
            return;
        }
        try {
            File l_fileToBeDeleted = new File(this.d_root, p_path.toString());
            if (l_fileToBeDeleted.isDirectory() && l_fileToBeDeleted.listFiles() != null
                    && l_fileToBeDeleted.listFiles().length == 0) {
                if (l_fileToBeDeleted.delete())
                    deleteIfEmptyFolder(p_path.parent());
            }
        } catch (IllegalArgumentException p_e) {
        }
    }
}
