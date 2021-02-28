package common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.*;

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
    private List<String> pathComponents;
    private final static String DELIMITER = "/";
    private final static String COLON = ":";

    /**
     * Creates a new path which represents the root directory.
     */
    public Path() {
        // root directory path
//        path.add("/")
        pathComponents = new ArrayList<>();
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
        if (component.isEmpty() || component.contains(DELIMITER) || component.contains(COLON)) {
            throw new IllegalArgumentException("Invalid component path");
        }
        // If provided path is empty
        pathComponents = path.pathComponents == null ?
                new ArrayList<>() :
                path.pathComponents;
        pathComponents.add(component);
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
        pathComponents = new ArrayList<>(Arrays.asList(path.split("/")));
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
        return pathComponents.iterator();
    }

    /**
     * Lists the paths of all files in a directory tree on the local filesystem.
     *
     * @param directory The root directory of the directory tree.
     * @return An array of relative paths, one for each file in the directory tree.
     * @throws FileNotFoundException    If the root directory does not exist.
     * @throws IllegalArgumentException If <code>directory</code> exists but does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException {
        if (directory.exists() && directory.isDirectory()) {
            List<Path> l_paths = new ArrayList<>();
            for (File filePath : Objects.requireNonNull(directory.listFiles())) {
                l_paths.add(new Path(filePath.getPath()));
            }
            return l_paths.toArray(new Path[0]);
        }
        throw new FileNotFoundException("Directory does not exists");
    }

    /**
     * Determines whether the path represents the root directory.
     *
     * @return <code>true</code> if the path does represent the root directory,
     * and <code>false</code> if it does not.
     */
    public boolean isRoot() {
        return this.pathComponents.size() == 0;
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
        Path parentPath = this;
        parentPath.pathComponents = parentPath.pathComponents.subList(0, pathComponents.lastIndexOf(DELIMITER));
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
        return pathComponents.get(pathComponents.size() - 1);
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
        for (String pathComponent : pathComponents) {
            if (indexOfSub >= other.pathComponents.size() ||
                    !pathComponent.equals(other.pathComponents.get(indexOfSub))) {
                return false;
            }
            indexOfSub++;
        }
        // If each path components are equal, both paths are equal.
        return true;
    }

    /**
     * Converts the path to <code>File</code> object.
     *
     * @param root The resulting <code>File</code> object is created relative to this directory.
     * @return The <code>File</code> object.
     */
    public File toFile(File root) {
        return new File(root, this.toString());
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
        return Objects.equals(pathComponents, l_l_strings.pathComponents);
    }

    /**
     * Returns the hash code of the path.
     */
    @Override
    public int hashCode() {
        return Objects.hash(pathComponents);
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
        StringBuilder pathString = new StringBuilder();
        if (pathComponents.size() == 0)
            return DELIMITER;
        for (String pathComponent : pathComponents) {
            pathString.append(DELIMITER.concat(pathComponent));
        }
        return pathString.toString();
    }
}