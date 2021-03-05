package naming;

import common.Path;
import rmi.RMIException;
import rmi.Skeleton;
import storage.Command;
import storage.Storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Naming server.
 *
 * <p>
 * Each instance of the filesystem is centered on a single naming server. The naming server maintains the filesystem
 * directory tree. It does not store any file data - this is done by separate storage servers. The primary purpose of
 * the naming server is to map each file name (path) to the storage server which hosts the file's contents.
 *
 * <p>
 * The naming server provides two interfaces, <code>Service</code> and
 * <code>Registration</code>, which are accessible through RMI. Storage servers
 * use the <code>Registration</code> interface to inform the naming server of their existence. Clients use the
 * <code>Service</code> interface to perform most filesystem operations. The documentation accompanying these
 * interfaces provides details on the methods supported.
 *
 * <p>
 * Stubs for accessing the naming server must typically be created by directly specifying the remote network address. To
 * make this possible, the client and registration interfaces are available at well-known ports defined in
 * <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration {
    private PathNode root;
    private Skeleton<Registration> d_registrationSkeleton;
    private Skeleton<Service> d_serviceSkeleton;
    private final List<ServerStubs> d_registeredServerStubs;
    private volatile boolean alive = false;

    /**
     * Creates the naming server object.
     *
     * <p>
     * The naming server is not started.
     */
    public NamingServer() {
        this.root = new PathNode(new Path(), null);
        this.d_registrationSkeleton = new Skeleton<>(Registration.class, this, new InetSocketAddress(NamingStubs.REGISTRATION_PORT));
        this.d_serviceSkeleton = new Skeleton<>(Service.class, this, new InetSocketAddress(NamingStubs.SERVICE_PORT));
        d_registeredServerStubs = new ArrayList<>();
    }

    /**
     * Starts the naming server.
     *
     * <p>
     * After this method is called, it is possible to access the client and registration interfaces of the naming server
     * remotely.
     *
     * @throws RMIException If either of the two skeletons, for the client or registration server interfaces, could not
     *                      be started. The user should not attempt to start the server again if an exception occurs.
     */
    public synchronized void start() throws RMIException {
        if (alive)
            throw new RMIException("Failed to start! Calling again may give the same result");
        synchronized (this) {
            this.alive = true;
        }
        try {
            d_registrationSkeleton.start();
            d_serviceSkeleton.start();
        } catch (RMIException p_rmiException) {
            synchronized (this) {
                alive = false;
            }
            throw new RMIException("Couldn't start naming server", p_rmiException);
        }
    }

    /**
     * Stops the naming server.
     *
     * <p>
     * This method waits for both the client and registration interface skeletons to stop. It attempts to interrupt as
     * many of the threads that are executing naming server code as possible. After this method is called, the naming
     * server is no longer accessible remotely. The naming server should not be restarted.
     */
    public void stop() {
        try {
            d_registrationSkeleton.stop();
            d_serviceSkeleton.stop();
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
     * Indicates that the server has completely shut down.
     *
     * <p>
     * This method should be overridden for error reporting and application exit purposes. The default implementation
     * does nothing.
     *
     * @param cause The cause for the shutdown, or <code>null</code> if the shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause) {
    }

    // The following methods are documented in Service.java.
    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException {
        if (new File(path.toString()).exists()) {
            return new File(path.toString()).isDirectory();
        }
        throw new FileNotFoundException();
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException {
        if (this.isDirectory(directory)) {
            return new File(directory.toString()).list();
        }
        return new String[0];
    }

    @Override
    public boolean createFile(Path file)
            throws RMIException, FileNotFoundException {
        if (file == null) {
            throw new NullPointerException("File is null parameter.");
        }
        PathNode l_currentPathNode = this.root;

        for (String component : file) {
            if (component.equals(file.last())) {
                if (!l_currentPathNode.getChildren().containsKey(component))
                    l_currentPathNode.addChild(component, null);
                else
                    return true;
            } else if (l_currentPathNode.getChildren().containsKey(component) && !l_currentPathNode.isFile()) {
                l_currentPathNode = l_currentPathNode.getChildren().get(component);
            } else {
                throw new FileNotFoundException("Parent directory not found!");
            }
        }
        return false;
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException {
        if (directory == null) {
            throw new NullPointerException("File is null parameter.");
        }
        PathNode l_currentPathNode = this.root;

        for (String component : directory) {
            if (component.equals(directory.last())) {
                if (!l_currentPathNode.getChildren().containsKey(component))
                    l_currentPathNode.addChild(component, null);
                else
                    return true;
            } else if (l_currentPathNode.getChildren().containsKey(component)) {
                l_currentPathNode = l_currentPathNode.getChildren().get(component);
            } else {
                throw new FileNotFoundException("Parent directory not found!");
            }
        }
        return false;
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException {
        if (path == null) {
            throw new NullPointerException("File is null parameter.");
        }
        PathNode l_currentPathNode = this.root;

        for (String component : path) {
            if (component.equals(path.last())) {
                if (l_currentPathNode.getChildren().containsKey(component))
                    l_currentPathNode.deleteChild(component);
                else
                    return false;
            } else if (l_currentPathNode.getChildren().containsKey(component)) {
                l_currentPathNode = l_currentPathNode.getChildren().get(component);
            } else {
                throw new FileNotFoundException("Parent directory not found!");
            }
        }
        return true;
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException {
        return this.root.getNodeByPath(file).getStubs().storageStub;
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files) {
        if (client_stub == null || command_stub == null || files == null) {
            throw new NullPointerException("Register function has null parameters.");
        }
        PathNode l_currentPathNode;
        ArrayList<Path> l_duplicates = new ArrayList<>();
        ServerStubs l_serverStubs = new ServerStubs(client_stub, command_stub);

        synchronized (d_registeredServerStubs) {
            if (d_registeredServerStubs.contains(l_serverStubs)) {
                throw new IllegalStateException("Storage server has been already registered.");
            }
            d_registeredServerStubs.add(l_serverStubs);
        }

        for (Path l_path : files) {
            l_currentPathNode = this.root;

            // TODO Assumption here is that no directory will have the same filename.
            String fileName = l_path.last();
            for (String path : l_path) {
                if (!path.equals(fileName) && l_currentPathNode.getChildren().containsKey(path)) {
                    l_currentPathNode = l_currentPathNode.getChildren().get(path);
                } else {
                    // No serverStub if path belongs to file.
                    if (path.equals(fileName)) {
                        try {
                            l_currentPathNode.addChild(path, null);
                        } catch (UnsupportedOperationException p_e) {
                            l_duplicates.add(l_path);
                        }
                    } else {
                        l_currentPathNode.addChild(path, new PathNode(l_path, l_serverStubs));
                    }
                }
            }
        }
        return l_duplicates.toArray(new Path[0]);
    }
}
