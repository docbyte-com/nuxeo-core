/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.commandline.executor.service;

import static org.apache.commons.collections4.ListUtils.union;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.nuxeo.runtime.model.Descriptor.UNIQUE_DESCRIPTOR_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.cmdtesters.CommandTestResult;
import org.nuxeo.ecm.platform.commandline.executor.service.cmdtesters.CommandTester;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.Executor;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.ShellExecutor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * POJO implementation of the {@link CommandLineExecutorService} interface. Also handles the Extension Point logic.
 *
 * @author tiry
 */
public class CommandLineExecutorComponent extends DefaultComponent implements CommandLineExecutorService {

    private static final Logger log = LogManager.getLogger(CommandLineExecutorComponent.class);

    public static final String EP_ENV = "environment";

    public static final String EP_CMD = "command";

    public static final String EP_CMDTESTER = "commandTester";

    public static final String DEFAULT_TESTER = "DefaultCommandTester";

    public static final String DEFAULT_EXECUTOR = "ShellExecutor";

    protected EnvironmentDescriptor env;

    protected Map<String, EnvironmentDescriptor> envDescriptors;

    protected Map<String, CommandTester> testers;

    protected Map<String, CommandLineDescriptor> commandDescriptors;

    protected Map<String, CommandAvailability> commandAvailabilities;

    protected Map<String, Executor> executors;

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        env = getDescriptor(EP_ENV, UNIQUE_DESCRIPTOR_ID);
        envDescriptors = this.<EnvironmentDescriptor> getDescriptors(EP_ENV)
                             .stream()
                             // filter out global env
                             .filter(descriptor -> isNotEmpty(descriptor.getName()))
                             .<EnvironmentDescriptor> mapMulti((descriptor, consumer) -> {
                                 for (String command : descriptor.getName().split(",")) {
                                     consumer.accept(descriptor.withName(command));
                                 }
                             })
                             .collect(Collectors.groupingBy(EnvironmentDescriptor::getName,
                                     Collectors.collectingAndThen(Collectors.toList(),
                                             list -> list.stream()
                                                         .reduce(new EnvironmentDescriptor(),
                                                                 EnvironmentDescriptor::merge))));
        testers = this.<CommandTesterDescriptor> getDescriptors(EP_CMDTESTER)
                      .stream()
                      .collect(Collectors.toMap(CommandTesterDescriptor::getName, descriptor -> {
                          try {
                              return descriptor.getTesterClass().getDeclaredConstructor().newInstance();
                          } catch (ReflectiveOperationException e) {
                              throw new RuntimeException("Unable to instantiate tester: " + descriptor.getName(), e);
                          }
                      }));
        commandDescriptors = this.<CommandLineDescriptor> getDescriptors(EP_CMD)
                                 .stream()
                                 .filter(CommandLineDescriptor::isEnabled)
                                 .collect(Collectors.toMap(CommandLineDescriptor::getName, Function.identity()));
        // compute the command availabilities
        commandAvailabilities = new HashMap<>();
        var testerCache = new HashMap<String, CommandTestResult>();
        for (var descriptor : commandDescriptors.values()) {
            String testerName = defaultIfNull(descriptor.getTester(), DEFAULT_TESTER);
            var tester = testers.get(testerName);
            if (tester == null) {
                log.error("Unable to find tester: {}, command will not be available: {}", testerName, name);
                commandAvailabilities.put(descriptor.getName(),
                        new CommandAvailability("Unable to find tester: " + testerName));
            } else {
                var testResult = testerCache.computeIfAbsent(
                        testerName + '-' + descriptor.getCommand() + '-' + descriptor.getTestParametersString(),
                        k -> tester.test(descriptor));
                if (testResult.succeed()) {
                    commandAvailabilities.put(descriptor.getName(), new CommandAvailability());
                } else {
                    commandAvailabilities.put(descriptor.getName(), new CommandAvailability(
                            descriptor.getInstallationDirective(), testResult.getErrorMessage()));
                }
            }
        }
        // logs about non available commands
        commandAvailabilities.entrySet()
                             .stream()
                             .filter(entry -> !entry.getValue().isAvailable())
                             .collect(Collectors.groupingBy(
                                     // group by shell command
                                     entry -> commandDescriptors.get(entry.getKey()).getCommand(),
                                     // reduce the nuxeo commands with the first error & installation messages
                                     Collectors.reducing(new NuxeoCommandsWithMessages(),
                                             entry -> new NuxeoCommandsWithMessages(entry.getValue().getErrorMessage(),
                                                     entry.getValue().getInstallMessage(), List.of(entry.getKey())),
                                             NuxeoCommandsWithMessages::merge)))
                             .forEach((shellCommand, nuxeoCommandsWithMessages) -> log.warn(
                                     "Shell command: {} is not available, the following Nuxeo commands won't be available: [{}], ({} - {})",
                                     () -> shellCommand,
                                     () -> String.join(", ", nuxeoCommandsWithMessages.nuxeoCommands()),
                                     nuxeoCommandsWithMessages::errorMessage,
                                     nuxeoCommandsWithMessages::installationDirective));
        // check the timeout command
        var useTimeout = commandAvailabilities.getOrDefault("timeout",
                new CommandAvailability("No timeout shell command available")).isAvailable();
        if (!useTimeout || SystemUtils.IS_OS_WINDOWS) {
            // Windows comes with a TIMEOUT.exe command but for different purpose
            useTimeout = false;
            log.warn("There is no timeout command available, command executions won't be time-boxed.");
        }
        executors = Map.of(DEFAULT_EXECUTOR, new ShellExecutor(useTimeout));
    }

    @Override
    public void stop(ComponentContext context) {
        env = null;
        envDescriptors = null;
        testers = null;
        commandDescriptors = null;
        commandAvailabilities = null;
        executors = null;
    }

    // Service interface

    @Override
    public ExecResult execCommand(String commandName, CmdParameters params) throws CommandNotAvailable {
        CommandAvailability availability = getCommandAvailability(commandName);
        if (!availability.isAvailable()) {
            throw new CommandNotAvailable(availability);
        }

        CommandLineDescriptor cmdDesc = commandDescriptors.get(commandName);
        Executor executor = executors.get(cmdDesc.getExecutor());
        EnvironmentDescriptor environment = new EnvironmentDescriptor().merge(
                env).merge(envDescriptors.getOrDefault(commandName, envDescriptors.get(cmdDesc.getCommand())));
        return executor.exec(cmdDesc, params, environment);
    }

    @Override
    public CommandAvailability getCommandAvailability(String commandName) {
        if (!commandDescriptors.containsKey(commandName)) {
            return new CommandAvailability(commandName + " is not a registered command");
        }
        return commandAvailabilities.get(commandName);
    }

    @Override
    public List<String> getRegistredCommands() {
        return new ArrayList<>(commandDescriptors.keySet());
    }

    @Override
    public List<String> getAvailableCommands() {
        return commandDescriptors.keySet()
                                 .stream()
                                 .filter(command -> commandAvailabilities.get(command).isAvailable())
                                 .toList();
    }

    @Override
    public CommandLineDescriptor getCommandLineDescriptor(String commandName) {
        return commandDescriptors.get(commandName);
    }

    // ******************************************
    // for testing

    /** @deprecated since 11.4, use instance method {@link #getCommandLineDescriptor} instead */
    @Deprecated
    public static CommandLineDescriptor getCommandDescriptor(String commandName) {
        return Framework.getService(CommandLineExecutorService.class).getCommandLineDescriptor(commandName);
    }

    @Override
    public CmdParameters getDefaultCmdParameters() {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("java.io.tmpdir", System.getProperty("java.io.tmpdir"));
        params.addNamedParameter(Environment.NUXEO_TMP_DIR, Environment.getDefault().getTemp().getPath());
        return params;
    }

    record NuxeoCommandsWithMessages(String errorMessage, String installationDirective, List<String> nuxeoCommands) {
        public NuxeoCommandsWithMessages() {
            this(null, null, List.of());
        }

        public NuxeoCommandsWithMessages merge(NuxeoCommandsWithMessages other) {
            var errorMessage = defaultIfNull(this.errorMessage, other.errorMessage);
            var installationDirective = defaultIfNull(this.installationDirective, other.installationDirective);
            var nuxeoCommands = union(this.nuxeoCommands, other.nuxeoCommands);
            return new NuxeoCommandsWithMessages(errorMessage, installationDirective, nuxeoCommands);
        }
    }
}
