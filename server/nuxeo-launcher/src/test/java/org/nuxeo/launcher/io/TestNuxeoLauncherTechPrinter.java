/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.launcher.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.nuxeo.connect.update.PackageDependency;
import org.nuxeo.connect.update.PackageState;
import org.nuxeo.connect.update.PackageType;
import org.nuxeo.launcher.info.CommandInfo;
import org.nuxeo.launcher.info.CommandSetInfo;
import org.nuxeo.launcher.info.ConfigurationInfo;
import org.nuxeo.launcher.info.DistributionInfo;
import org.nuxeo.launcher.info.InstanceInfo;
import org.nuxeo.launcher.info.KeyValueInfo;
import org.nuxeo.launcher.info.PackageInfo;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xmlunit.assertj3.XmlAssert;

/**
 * @since 2025.0
 */
@RunWith(Parameterized.class)
public class TestNuxeoLauncherTechPrinter {

    protected static final Logger log = LogManager.getLogger(TestNuxeoLauncherTechPrinter.class);

    @Rule
    public final TestName name = new TestName();

    protected final NuxeoLauncherTechPrinter printer;

    protected final BiConsumer<String, String> checker;

    public TestNuxeoLauncherTechPrinter(NuxeoLauncherTechPrinter.Format format) {
        this.printer = new NuxeoLauncherTechPrinter(format);
        this.checker = switch (format) {
            case JSON -> (expected, actual) -> JSONAssert.assertEquals(expected, actual, true);
            case XML ->
                (expected, actual) -> XmlAssert.assertThat(actual).and(expected).ignoreWhitespace().areIdentical();
        };
    }

    @Test
    public void testEmptyInstanceInfo() throws Exception {
        var info = new InstanceInfo();

        String output = printObject(info);
        assertContent(output);
    }

    // NXP-29401
    @Test
    public void testPackageInfoWithNewLineInDescription() throws Exception {
        var packageInfo = new PackageInfo();
        packageInfo.description = """
                Download and install the latest hotfix to keep your Nuxeo up-to-date.
                    Changes will take effects after restart.""";

        var instanceInfo = new InstanceInfo();
        instanceInfo.packages.add(packageInfo);

        String output = printObject(instanceInfo);
        assertContent(output);
    }

    @Test
    public void testPackageInfoWithDependencies() throws Exception {
        var packageInfoOneDep = new PackageInfo();
        packageInfoOneDep.dependencies = new PackageDependency[] { new PackageDependency("dependency-1") };

        var packageInfoSeveralDep = new PackageInfo();
        packageInfoSeveralDep.dependencies = new PackageDependency[] { new PackageDependency("dependency-1"),
                new PackageDependency("dependency-2") };

        var instanceInfo = new InstanceInfo();
        instanceInfo.packages.add(packageInfoOneDep);
        instanceInfo.packages.add(packageInfoSeveralDep);

        String output = printObject(instanceInfo);
        assertContent(output);
    }

    @Test
    public void testPackageInfoWithTemplates() throws Exception {
        var packageInfoOneDep = new PackageInfo();
        packageInfoOneDep.templates = new LinkedHashSet<>(List.of("template-1"));

        var packageInfoSeveralDep = new PackageInfo();
        packageInfoSeveralDep.templates = new LinkedHashSet<>(List.of("template-1", "template-2"));

        var instanceInfo = new InstanceInfo();
        instanceInfo.packages.add(packageInfoOneDep);
        instanceInfo.packages.add(packageInfoSeveralDep);

        String output = printObject(instanceInfo);
        assertContent(output);
    }

    @Test
    public void testFullInstanceInfo() throws Exception {
        var distribution = new DistributionInfo();
        distribution.name = "lts";
        distribution.server = "tomcat";
        distribution.version = "2025.0";
        distribution.date = "20240407";
        distribution.packaging = "docker";

        var packageInfo = newWebUiPackageInfo(PackageState.STARTED);

        var configuration = new ConfigurationInfo();
        configuration.basetemplates.addAll(List.of("common-base", "common"));
        configuration.keyvals.add(new KeyValueInfo("nuxeo.force.generation", "true"));
        configuration.allkeyvals.add(new KeyValueInfo("file.separator", "/"));

        var info = new InstanceInfo();
        info.NUXEO_CONF = "/some/path/bin/nuxeo.conf";
        info.NUXEO_HOME = "/some/path";
        info.clid = "some-clid";
        info.distribution = distribution;
        info.packages.add(packageInfo);
        info.config = configuration;

        String output = printObject(info);
        assertContent(output);
    }

    @Test
    public void testCommandSetInfoMpList() throws Exception {
        var customLoggerPackage = new PackageInfo();
        customLoggerPackage.name = "custom-logger-activator";
        customLoggerPackage.version = "0.0.5";
        customLoggerPackage.id = "custom-logger-activator-0.0.5";
        customLoggerPackage.state = PackageState.STARTED;
        customLoggerPackage.title = "Custom Logger Marketplace package";
        customLoggerPackage.description = "Nuxeo Marketplace to activate a special logger.";
        customLoggerPackage.licenseType = "Apache License, Version 2.0";
        customLoggerPackage.licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0";
        customLoggerPackage.targetPlatforms = new String[] { "lts-2021.*", "lts-2023.*" };
        customLoggerPackage.type = PackageType.ADDON;
        customLoggerPackage.supportsHotReload = false;

        var webUiPackage = newWebUiPackageInfo(PackageState.STARTED);

        var commandInfo = new CommandInfo();
        commandInfo.name = "list";
        commandInfo.exitCode = 0;
        commandInfo.pending = false;
        commandInfo.packages.add(customLoggerPackage);
        commandInfo.packages.add(webUiPackage);

        var commandSetInfo = new CommandSetInfo();
        commandSetInfo.commands.add(commandInfo);

        String output = printObject(commandSetInfo);
        assertContent(output);
    }

    @Test
    public void testCommandSetInfoMpRemoveOnStartedPackage() throws Exception {
        var webUiPackage = newWebUiPackageInfo(PackageState.DOWNLOADED);
        var uninstallCommand = new CommandInfo();
        uninstallCommand.name = "uninstall";
        uninstallCommand.param = "nuxeo-web-ui-3.1.7";
        uninstallCommand.exitCode = 0;
        uninstallCommand.pending = false;
        uninstallCommand.packages.add(webUiPackage);

        webUiPackage = newWebUiPackageInfo(PackageState.REMOTE);
        var removeCommand = new CommandInfo();
        removeCommand.name = "remove";
        removeCommand.param = "nuxeo-web-ui-3.1.7";
        removeCommand.exitCode = 0;
        removeCommand.pending = false;
        removeCommand.packages.add(webUiPackage);

        var commandSetInfo = new CommandSetInfo();
        commandSetInfo.commands.add(uninstallCommand);
        commandSetInfo.commands.add(removeCommand);

        String output = printObject(commandSetInfo);
        assertContent(output);

    }

    protected String printObject(Object object) throws IOException {
        try (OutputStream os = new ByteArrayOutputStream()) {
            printer.print(object, os);
            return os.toString();
        }
    }

    protected void assertContent(String actual) throws IOException {
        String expectedFile = "printer/" + name.getMethodName().replaceFirst("\\[.+]", "") + "."
                + printer.format.name().toLowerCase();
        File file = org.nuxeo.common.utils.FileUtils.getResourceFileFromContext(expectedFile);
        if (file == null || !file.exists()) {
            throw new RuntimeException("Unable to find file: " + expectedFile + ", please create it");
        }
        String expected = FileUtils.readFileToString(file, UTF_8);
        try {
            checker.accept(expected, actual);
        } catch (AssertionError e) {
            log.error("""
                    Assertion has failed:
                    Expected:
                    {}

                    Actual:
                    {}
                    """, expected.indent(2), actual.indent(2));
            throw e;
        }
    }

    protected PackageInfo newWebUiPackageInfo(PackageState state) {
        var packageInfo = new PackageInfo();
        packageInfo.name = "nuxeo-web-ui";
        packageInfo.version = "3.1.7";
        packageInfo.id = "nuxeo-web-ui-3.1.7";
        packageInfo.state = state;
        packageInfo.title = "Nuxeo Web UI";
        packageInfo.description = "Web UI is the primary UI for the Nuxeo Platform. It is an ideal starting point for any Digital Asset Management, Case Management or Document Management project.";
        packageInfo.licenseType = "Apache License, Version 2.0";
        packageInfo.licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html";
        packageInfo.type = PackageType.ADDON;
        packageInfo.supportsHotReload = false;
        return packageInfo;
    }

    @Parameters(name = "{0}")
    public static Object[] data() {
        return NuxeoLauncherTechPrinter.Format.values();
    }
}
