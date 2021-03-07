package common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Distributed filesystem paths.
 *
 * <p>
 * Objects of type <code>Path</code> are used by all filesystem interfaces. Path objects are immutable.
 *
 * <p>
 * The string representation of paths is a forward-slash-delimited sequence of path components. The root directory is
 * represented as a single forward slash.
 *
 * <p>
 * The colon (<code>:</code>) and forward slash (<code>/</code>) characters are not permitted within path components.
 * The forward slash is the delimiter, and the colon is reserved as a delimiter for application use.
 */
public class Path implements Iterable<String>, Serializable {
    private List<String> d_pathComponents;
    private final static String DELIMITER = "/";
    private final static String COLON = ":";

    /**
     * Creates a new path which represents the root directory.
     */
    public Path() {
        // root directory path
        d_pathComponents = new ArrayList<>();
    }

    /**
     * Creates a new path by appending the given component to an existing path.
     *
     * @param path      The existing path.
     * @param component The new component.
     * @throws IllegalArgumentException If <code>component</code> includes the separator, a colon, or
     *                                  <code>component</code> is the empty
     *                                  string.
     */
    public Path(Path path, String component) {
        if (component == null || component.isEmpty() ||
                component.contains(DELIMITER) || component.contains(COLON)) {
            throw new IllegalArgumentException("Invalid component path");
        }
        d_pathComponents = new ArrayList<>();
        if (path != null) {
            for (String l_path : path) {
                d_pathComponents.add(l_path);
            }
        }
        d_pathComponents.add(component);
    }

    /**
     * Creates a new path from a path string.
     *
     * <p>
     * The string is a sequence of components delimited with forward slashes. Empty components are dropped. The string
     * must begin with a forward slash.
     *
     * @param path The path string.
     * @throws IllegalArgumentException If the path string does not begin with a forward slash, or if the path contains
     *                                  a colon character.
     */
    public Path(String path) {
        if (path == null || path.isEmpty() || path.contains(COLON)) {
            throw new IllegalArgumentException("Invalid component path");
        }
        if (!path.startsWith(DELIMITER))
            throw new IllegalArgumentException("Path string does not begin with a forward slash!");
        d_pathComponents = new ArrayList<>(Arrays.asList(path.split(DELIMITER)));
        // Remove if any empty string of if any spaces of sub-path.
        d_pathComponents = d_pathComponents.stream().filter(p_pathComponent ->
                !p_pathComponent.trim().isEmpty()
        ).collect(Collectors.toList());
    }

    /**
     * Returns an iterator over the components of the path.
     *
     * <p>
     * The iterator cannot be used to modify the path object - the
     * <code>remove</code> method is not supported.
     *
     * @return The iterator.
     */
    @Override
    public Iterator<String> iterator() {
        // As remove method is not supported, return an iterator from the unmodifiable list.
        return Collections.unmodifiableList(d_pathComponents).iterator();
    }

    /**
     * Lists the paths of all files in a directory tree on the local filesystem.
     *
     * @param p_directory The root directory of the directory tree.
     * @return An array of relative paths, one for each file in the directory tree.
     * @throws FileNotFoundException    If the root directory does not exist.
     * @throws IllegalArgumentException If <code>directory</code> exists but does not refer to a directory.
     */
    public static Path[] list(File p_directory) throws FileNotFoundException {
        if (!p_directory.exists()) {
            throw new FileNotFoundException("Directory does not exists!");
        }
        if (!p_directory.isDirectory()) {
            throw new IllegalArgumentException("Parameter does not refer to a directory!");
        }
        URI directoryURI = p_directory.toURI();
        List<File> l_files = new ArrayList<>();
        list(p_directory, l_files);

        List<Path> l_paths = new ArrayList<>();
        for (File l_file : Objects.requireNonNull(l_files)) {
            URI fileURI = l_file.toURI();
            URI relativePath = directoryURI.relativize(fileURI);
            l_paths.add(new Path(DELIMITER.concat(relativePath.getPath())));
        }
        return l_paths.toArray(new Path[0]);
    }

    /**
     * Private recursive method to get the list of files from the given directory.
     *
     * @param p_directory Value of directory to be traversed.
     * @param p_files     Reference of the list where the traversed files to be added.
     */
    private static void list(File p_directory, List<File> p_files) {
        // Get all files from a directory.
        File[] fList = p_directory.listFiles();
        if (fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    p_files.add(file);
                } else if (file.isDirectory()) {
                    Path.list(new File(file.getAbsolutePath()), p_files);
                }
            }
    }

    /**
     * Determines whether the path represents the root directory.
     *
     * @return <code>true</code> if the path does represent the root directory,
     * and <code>false</code> if it does not.
     */
    public boolean isRoot() {
        return this.d_pathComponents.size() == 0;
    }

    /**
     * Returns the path to the parent of this path.
     *
     * @throws IllegalArgumentException If the path represents the root directory, and therefore has no parent.
     */
    public Path parent() {
        if (this.isRoot()) {
            throw new IllegalArgumentException("Path represents the root directory");
        }
        Path parentPath = new Path(this.toString());
        int secondLastIndex = parentPath.d_pathComponents.size() - 1;
        if (secondLastIndex < 0) {
            return new Path();
        }
        // Exclusive secondLastIndex
        parentPath.d_pathComponents = parentPath.d_pathComponents.subList(0, secondLastIndex);
        return parentPath;
    }

    /**
     * Returns the last component in the path.
     *
     * @throws IllegalArgumentException If the path represents the root directory, and therefore has no last component.
     */
    public String last() {
        if (this.isRoot()) {
            throw new IllegalArgumentException("Path represents the root directory");
        }
        return d_pathComponents.get(d_pathComponents.size() - 1);
    }

    /**
     * Determines if the given path is a subpath of this path.
     *
     * <p>
     * The other path is a subpath of this path if is a prefix of this path. Note that by this definition, each path is
     * a subpath of itself.
     *
     * @param other The path to be tested.
     * @return <code>true</code> If and only if the other path is a subpath of
     * this path.
     */
    public boolean isSubpath(Path other) {
        int indexOfSub = 0;
        int trackPathCount = 0;
        if (d_pathComponents.size() < other.d_pathComponents.size()) {
            return false;
        }
        for (String pathComponent : d_pathComponents) {
            try {
                if (other.d_pathComponents.get(indexOfSub).equals(pathComponent)) {
                    trackPathCount++;
                }
            } catch (IndexOutOfBoundsException p_index) {
                break;
            }
            indexOfSub++;
        }
        // If each path components are equal, both paths are equal.
        return trackPathCount == indexOfSub;
    }

    /**
     * Converts the path to <code>File</code> object.
     *
     * @param root The resulting <code>File</code> object is created relative to this directory.
     * @return The <code>File</code> object.
     */
    public File toFile(File root) {
        return new File(root.toURI().relativize(URI.create(this.toString())).toString());
    }

    /**
     * Length of the path.
     *
     * @return Value of the length of path.
     */
    public int length() {
        return this.d_pathComponents.size();
    }

    /**
     * Compares two paths for equality.
     *
     * <p>
     * Two paths are equal if they share all the same components.
     *
     * @param other The other path.
     * @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Path l_l_strings = (Path) other;
        return Objects.equals(d_pathComponents, l_l_strings.d_pathComponents);
    }

    /**
     * Returns the hash code of the path.
     */
    @Override
    public int hashCode() {
        return Objects.hash(d_pathComponents);
    }

    /**
     * Converts the path to a string.
     *
     * <p>
     * The string may later be used as an argument to the
     * <code>Path(String)</code> constructor.
     *
     * @return The string representation of the path.
     */
    @Override
    public String toString() {
        // If the path represents the root.
        if (d_pathComponents.size() == 0)
            return DELIMITER;

        StringBuilder pathString = new StringBuilder();
        // Iterate over the path component list
        for (String pathComponent : d_pathComponents) {
            pathString.append(DELIMITER.concat(pathComponent));
        }
        return pathString.toString();
    }
}
