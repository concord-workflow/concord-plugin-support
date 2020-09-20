package ca.vanzyl.concord.plugins.toolsupport;

import ca.vanzyl.concord.plugins.Configurator;
import ca.vanzyl.concord.plugins.tool.CliCommand;
import ca.vanzyl.concord.plugins.tool.ImmutableToolDescriptor;
import ca.vanzyl.concord.plugins.tool.OS;
import ca.vanzyl.concord.plugins.tool.PackageResolver;
import ca.vanzyl.concord.plugins.tool.ToolConfiguration;
import ca.vanzyl.concord.plugins.tool.ToolDescriptor;
import ca.vanzyl.concord.plugins.tool.ToolInitializationResult;
import ca.vanzyl.concord.plugins.tool.ToolInitializer;
import ca.vanzyl.concord.plugins.tool.annotations.AnnotationProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.walmartlabs.concord.runtime.v2.sdk.Context;
import com.walmartlabs.concord.runtime.v2.sdk.TaskResult;
import com.walmartlabs.concord.sdk.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

public final class ToolTaskSupport
{
    private static final Logger logger = LoggerFactory.getLogger(ToolTaskSupport.class);

    private final String taskName;
    private final Configurator toolConfigurator;
    private final ToolInitializer toolInitializer;
    private final AnnotationProcessor annotationProcessor;

    public ToolTaskSupport(String taskName, PackageResolver packageResolver)
    {
        this.taskName = taskName;
        this.toolConfigurator = new Configurator();
        this.toolInitializer = new ToolInitializer(packageResolver);
        this.annotationProcessor = new AnnotationProcessor();
    }

    public TaskResult execute(Context ctx, Map<String, ToolCommandV2> commands, Map<String, Object> input)
            throws Exception
    {
        Path workDir = ctx.workingDirectory();

        // Retrieve the name of the command from the configuration
        String toolCommandName = MapUtils.assertString(input, "command");

        // Retrieve the configuration as a createConfiguration from the context

        // Retrieve the common configuration elements for all commands:
        //
        // version
        // url
        // debug
        // dryRun
        //
        ToolConfiguration toolConfiguration = toolConfigurator.createConfiguration(input, ToolConfiguration.class);

        // Retrieve the specific command as specified by the "command" key in the configuration
        ToolCommandV2 toolCommand = commands.get(taskName + "/" + toolCommandName);
        if (toolCommand == null) {
            throw new RuntimeException(String.format("Cannot find the command %s/%s", taskName, toolCommandName));
        }

        // Apply the configuration to the command
        toolConfigurator.configure(toolCommand, input);

        ToolDescriptor toolDescriptor = toolDescriptor(taskName, toolConfiguration);

        // Initialize the specific tool and make it available to concord for use
        ToolInitializationResult toolInitializationResult = toolInitializer.initialize(workDir, toolDescriptor, toolConfiguration.debug());

        logger.info("We have successfully initialized {} version {}.", toolDescriptor.name(), toolDescriptor.version() != null ? toolDescriptor.version() : toolDescriptor.defaultVersion());

        // Build up the arguments for the execution of this tool: executable +
        List<String> cliArguments = Lists.newArrayList();
        cliArguments.add(toolInitializationResult.executable().toFile().getAbsolutePath());
        cliArguments.addAll(annotationProcessor.process(toolCommandName, toolCommand));

        Map<String, Object> taskResult = new HashMap<>();

        // Here is where we want to alter what Helm install is doing. If there is an externals configuration we want
        // fetch the Helm chart, insert the externals into the Helm chart and then install from the directory we
        // created with the fetched Helm chart
        //
        // - do any preparation work here and run any commands necessary. i need the path to the executable and access
        //   to the command and its configuration
        // - change the command line arguments as necessary. in the case of Helm we need to install from the directory
        //   just created.

        // Place envar into the context
        taskResult.put("envars", toolConfiguration.envars());

        //String commandLineArguments = String.join(" ", cliArguments);
        if (toolConfiguration.dryRun()) {

            // Command pre-processing
            toolCommand.preProcess(ctx, workDir);
            toolCommand.preExecute(workDir, cliArguments);

            // Command post-processing
            toolCommand.postProcess(workDir);
            toolCommand.postExecute(workDir);

            String commandLineArguments = String.join(" ", cliArguments);
            logger.info(commandLineArguments);

            addContextVariables(taskResult, toolInitializationResult, commandLineArguments);
        }
        else {
            String idempotencyCheckCommand = toolCommand.idempotencyCheckCommand(ctx);
            if (idempotencyCheckCommand != null) {
                idempotencyCheckCommand = mustache(idempotencyCheckCommand, "executable", toolInitializationResult.executable().toFile().getAbsolutePath());
                logger.info("idempotencyCheckCommand: " + idempotencyCheckCommand);

                // "{{executable}} get cluster --name {{name}} --region {{region}} -o json"
                CliCommand idempotencyCheck = new CliCommand(idempotencyCheckCommand, workDir, toolConfiguration.envars(), toolConfiguration.saveOutput());
                CliCommand.Result result = idempotencyCheck.execute(Executors.newCachedThreadPool());

                if (result.getCode() == toolCommand.expectedIdempotencyCheckReturnValue()) {
                    logger.info("This command has already run successfully: " + cliArguments);
                    // The task we are intending to run has already executed successfully. It is the job of the idempotency
                    // command to ask if what we intend to do has already been done.
                    return TaskResult.success().values(taskResult);
                }
            }

            // Command pre-processing
            toolCommand.preProcess(ctx, workDir);
            toolCommand.preExecute(workDir, cliArguments);
            String commandLineArguments = String.join(" ", cliArguments);
            logger.info("Executing: " + commandLineArguments);
            CliCommand command = new CliCommand(cliArguments, workDir, toolConfiguration.envars(), toolConfiguration.saveOutput());
            CliCommand.Result commandResult = command.execute(Executors.newCachedThreadPool());
            if (commandResult.getCode() != 0) {
                throw new RuntimeException(String.format("The command %s failed. Look in the logs above.", commandLineArguments));
            }

            // Used for testing to collect the log output to make assertions against
            if (toolConfiguration.saveOutput()) {
                addContextVariables(taskResult, toolInitializationResult, commandLineArguments);
                taskResult.put("logs", commandResult.getStdout());
            }

            // Command post-processing
            toolCommand.postProcess(workDir);
            toolCommand.postExecute(workDir);
        }

        return TaskResult.success().values(taskResult);
    }

    protected void addContextVariables(Map<String, Object> result, ToolInitializationResult toolInitializationResult, String commandLineArguments)
    {
        result.put("executable", toolInitializationResult.executable().toString());
        result.put("commandLineArguments", commandLineArguments);
        result.put("normalizedCommandLineArguments", commandLineArguments.substring(toolInitializationResult.executable().toString().lastIndexOf('/') + 1));
        result.put("dryRun", "true");
    }

    public static ToolDescriptor toolDescriptor(String taskName, ToolConfiguration toolConfiguration)
            throws Exception
    {

        ToolDescriptor toolDescriptor = fromResource(taskName);

        // Update the version if overriden by the user
        if (toolConfiguration.version() != null) {
            toolDescriptor = ImmutableToolDescriptor.copyOf(toolDescriptor).withVersion(toolConfiguration.version());
        }

        // Update the url if overriden by the user
        if (toolConfiguration.url() != null) {
            toolDescriptor = ImmutableToolDescriptor.copyOf(toolDescriptor).withUrlTemplate(toolConfiguration.url());
        }

        return toolDescriptor;
    }

    public static ToolDescriptor fromResource(String taskName)
            throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream inputStream = getResourceDescriptor(taskName)) {
            return mapper.readValue(inputStream, ToolDescriptor.class);
        }
    }

    public static InputStream getResourceDescriptor(String taskName)
    {
        ClassLoader classLoader = ToolTaskSupport.class.getClassLoader();
        return Optional.ofNullable(classLoader.getResourceAsStream(taskName + "/" + OS.CURRENT.getOsName() + ".descriptor.yml"))
                .orElse(classLoader.getResourceAsStream(taskName + "/" + "descriptor.yml"));
    }

    private static String mustache(String s, String var, String replacement)
    {
        return s.replaceAll("\\{\\{" + var + "}\\}", replacement);
    }
}
