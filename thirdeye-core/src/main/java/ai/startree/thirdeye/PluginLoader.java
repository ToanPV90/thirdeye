/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye;

import static ai.startree.thirdeye.spi.util.SpiUtils.optional;
import static java.util.Objects.requireNonNull;

import ai.startree.thirdeye.datasource.DataSourcesLoader;
import ai.startree.thirdeye.detection.annotation.registry.DetectionRegistry;
import ai.startree.thirdeye.notification.NotificationServiceRegistry;
import ai.startree.thirdeye.spi.Plugin;
import ai.startree.thirdeye.spi.PluginClassLoader;
import ai.startree.thirdeye.spi.datasource.ThirdEyeDataSourceFactory;
import ai.startree.thirdeye.spi.detection.AnomalyDetectorFactory;
import ai.startree.thirdeye.spi.detection.EventTriggerFactory;
import ai.startree.thirdeye.spi.notification.NotificationServiceFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads all plugins from a plugins dir as configured in {@link PluginLoaderConfiguration}
 * Expected Directory Structure:
 * - plugins/
 * -        /exampleplugin
 *
 * Plugins can have jars and resource files in the plugin directory which is loaded using
 * a {@link URLClassLoader} using the {@link ServiceLoader} interface.
 */
@Singleton
public class PluginLoader {

  private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);

  private final DataSourcesLoader dataSourcesLoader;
  private final DetectionRegistry detectionRegistry;
  private final NotificationServiceRegistry notificationServiceRegistry;

  private final AtomicBoolean loading = new AtomicBoolean();
  private final File pluginsDir;

  @Inject
  public PluginLoader(
      final DataSourcesLoader dataSourcesLoader,
      final DetectionRegistry detectionRegistry,
      final NotificationServiceRegistry notificationServiceRegistry,
      final PluginLoaderConfiguration config) {
    this.dataSourcesLoader = dataSourcesLoader;
    this.detectionRegistry = detectionRegistry;
    this.notificationServiceRegistry = notificationServiceRegistry;
    pluginsDir = new File(config.getPluginsPath());
  }

  public void loadPlugins() {
    if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
      log.error("Skipping Plugin Loading. Plugin dir not found: " + pluginsDir);
      return;
    }

    if (loading.compareAndSet(false, true)) {
      final File[] files = requireNonNull(pluginsDir.listFiles());
      for (File pluginDir : files) {
        if (pluginDir.isDirectory()) {
          loadPlugin(pluginDir);
        }
      }
    }
  }

  private void loadPlugin(final File pluginDir) {
    log.info("Loading plugin: " + pluginDir);
    final URLClassLoader pluginClassLoader = createPluginClassLoader(pluginDir);
    final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(pluginClassLoader);
      for (Plugin plugin : ServiceLoader.load(Plugin.class, pluginClassLoader)) {
        installPlugin(plugin);
      }
    } finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  private void installPlugin(final Plugin plugin) {
    log.info("Installing plugin: " + plugin.getClass().getName());
    for (ThirdEyeDataSourceFactory f : plugin.getDataSourceFactories()) {
      dataSourcesLoader.addThirdEyeDataSourceFactory(f);
    }
    for (AnomalyDetectorFactory f : plugin.getAnomalyDetectorFactories()) {
      detectionRegistry.addAnomalyDetectorFactory(f);
    }
    for (EventTriggerFactory f : plugin.getEventTriggerFactories()) {
      detectionRegistry.addEventTriggerFactory(f);
    }
    for (NotificationServiceFactory f : plugin.getNotificationServiceFactories()) {
      notificationServiceRegistry.addNotificationServiceFactory(f);
    }
  }

  private URLClassLoader createPluginClassLoader(File dir) {
    final URL[] urls = Arrays.stream(optional(dir.listFiles()).orElse(new File[]{}))
        .sorted()
        .map(File::toURI)
        .map(this::toUrl)
        .toArray(URL[]::new);

    return new PluginClassLoader(urls, getClass().getClassLoader());
  }

  private URL toUrl(final URI uri) {
    try {
      return uri.toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
