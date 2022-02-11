/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */
package ai.startree.thirdeye.spi;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Loads a plugin leveraging a {@link URLClassLoader}. However, it restricts the plugin from
 * using the system classloader thereby trimming access to all system classes.
 *
 * Only the classes in SHARED_PACKAGES are visible to the plugin.
 *
 */
public class PluginClassLoader extends URLClassLoader {

  private static final ClassLoader PLATFORM_CLASS_LOADER = platformClassLoaderIfExists();

  public static final ImmutableList<String> SHARED_PACKAGES = ImmutableList.<String>builder()
      .add("ai.startree.thirdeye.spi")
      .add("com.google.common")
      .add("org.joda.time")
      .add("org.slf4j")
      .add("org.h2")
      .add("com.mysql")
      .add("javax.activation")
      .build();

  private final ClassLoader parentClassLoader;

  public PluginClassLoader(final URL[] urls, final ClassLoader parentClassLoader) {
    super(urls, PLATFORM_CLASS_LOADER);
    this.parentClassLoader = parentClassLoader;
  }

  /**
   * For Java 8 or earlier, sending 'null' as the parent classloader works fine for loading
   * classes using the app classloader. For java 9 and above, this is done using a different API.
   *
   * This approach is also used in other codebases.
   * https://github.com/prestodb/presto/blob/master/presto-main/src/main/java/com/facebook/presto/server/PluginClassLoader.java
   *
   * @return platform class loader if available.
   */
  @SuppressWarnings("JavaReflectionMemberAccess")
  private static ClassLoader platformClassLoaderIfExists() {
    try {
      // Return the platform class loader if available
      // For Java 8 and earlier, this method is not available and
      final Method method = ClassLoader.class.getMethod("getPlatformClassLoader");
      return (ClassLoader) method.invoke(null);
    } catch (final NoSuchMethodException ignored) {
      // use null class loader on Java 8
      return null;
    } catch (final IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Class<?> loadClass(final String name, final boolean resolve)
      throws ClassNotFoundException {
    // has the class loaded already?
    Class<?> loadedClass = findLoadedClass(name);
    if (loadedClass == null) {
      final boolean isSharedClass = SHARED_PACKAGES.stream().anyMatch(name::startsWith);
      if (isSharedClass) {
        loadedClass = parentClassLoader.loadClass(name);
      } else {
        loadedClass = super.loadClass(name, resolve);
      }
    }

    if (resolve) {      // marked to resolve
      resolveClass(loadedClass);
    }
    return loadedClass;
  }
}
