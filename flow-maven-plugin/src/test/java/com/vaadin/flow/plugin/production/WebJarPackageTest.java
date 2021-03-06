package com.vaadin.flow.plugin.production;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.plugin.common.ArtifactData;

import static org.junit.Assert.assertEquals;

/**
 * @author Vaadin Ltd.
 */
public class WebJarPackageTest {
    @Rule
    public TemporaryFolder testDirectory = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private File webJarFile;

    @Test
    public void selectCorrectPackage_differentNames() {
        String version = "1.0.2";
        WebJarPackage one = createPackage("one", version);
        WebJarPackage two = createPackage("two", version);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(String.format(
                "Cannot process packages with different names: '%s' and '%s'",
                one.getPackageName(), two.getPackageName()));

        WebJarPackage.selectCorrectPackage(one, two);
    }

    @Test
    public void selectCorrectPackage_differentVersions() {
        String packageName = "vaaadin-test";
        WebJarPackage packageOne = createPackage(packageName, "22222");
        WebJarPackage packageTwo = createPackage(packageName, "111");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(String.format(
                "Two webJars have same name and different versions: '%s' and '%s', there should be no version differences",
                packageOne.getWebJar(), packageTwo.getWebJar()));

        WebJarPackage.selectCorrectPackage(packageOne, packageTwo);
    }

    @Test
    public void selectCorrectPackage_sameVersionsOnePrefixed() {
        String packageName = "vaaadin-test";
        String version = "1.0.3";
        String prefixedVersion = 'v' + version;
        WebJarPackage packageWithoutPrefixedVersion = createPackage(packageName,
                version);

        WebJarPackage merged = WebJarPackage.selectCorrectPackage(
                packageWithoutPrefixedVersion,
                createPackage(packageName, prefixedVersion));

        assertEquals("Expected to have version without prefix after merge",
                merged.getPackageName(),
                packageWithoutPrefixedVersion.getPackageName());
        assertEquals("Got different package name after merge",
                merged.getPathToPackage(),
                packageWithoutPrefixedVersion.getPathToPackage());
        assertEquals("Got different WebJar after merge", merged.getWebJar(),
                packageWithoutPrefixedVersion.getWebJar());
    }

    private WebJarPackage createPackage(String name, String version) {
        if (webJarFile == null) {
            try {
                webJarFile = testDirectory.newFile("testWebJarFile");
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to create test web jar file", e);
            }
        }
        return new WebJarPackage(
                new ArtifactData(webJarFile, "artifactId", version), name,
                "path");
    }
}
