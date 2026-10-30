/**
 * Child-first loader for plugins: each plugin can bundle its own library versions,
 * while the shared API and the JDK always come from the host (so types are compatible).
 */
final class PluginClassLoader extends URLClassLoader {
    private static final List<String> HOST_FIRST = List.of("java.", "javax.", "com.myapp.plugin.api.");

    PluginClassLoader(URL[] jars, ClassLoader host) { super(jars, host); }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            if (HOST_FIRST.stream().anyMatch(name::startsWith)) return super.loadClass(name, resolve);
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try { c = findClass(name); }                       // plugin JAR first
                catch (ClassNotFoundException e) { return super.loadClass(name, resolve); }
            }
            if (resolve) resolveClass(c);
            return c;
        }
    }
}
// Closing the loader and dropping every reference to its classes lets the GC unload them —
// a lingering reference (static cache, thread, ThreadLocal) leaks the whole loader: a Metaspace leak.
