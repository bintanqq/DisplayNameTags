package com.mattmx.nametags;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@SuppressWarnings("UnstableApiUsage")
public class NameTagsLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        classpathBuilder.getContext().getLogger().info("Injecting dependencies");

        // File to override version
        final File override = classpathBuilder.getContext()
            .getDataDirectory()
            .resolve(".override")
            .toFile();

        // EntityLib is published to Maven Central with groupId io.github.tofaa2
        String entityLibVersion = "3.0.3-SNAPSHOT";
        if (override.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(override))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    entityLibVersion = line.trim();
                }
            } catch (Exception error) {
                classpathBuilder.getContext().getLogger().warn("Failed to read .override file: " + error.getMessage());
            }
        }

        MavenLibraryResolver resolver = new MavenLibraryResolver();

        // EntityLib is available on Maven Central
        // No need to add custom repositories as mavenCentral() is included by default

        resolver.addDependency(
            new Dependency(
                new DefaultArtifact("io.github.tofaa2:spigot:" + entityLibVersion),
                null
            ).setOptional(false)
        );

        classpathBuilder.addLibrary(resolver);
    }

}