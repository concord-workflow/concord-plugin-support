package ca.vanzyl.concord.plugins.toolsupport;

import com.walmartlabs.concord.runtime.v2.sdk.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public abstract class ToolCommandSupportV2
        implements ToolCommandV2
{
    private static final Logger logger = LoggerFactory.getLogger(ToolCommandSupportV2.class);

    @Override
    public String idempotencyCheckCommand(Context ctx)
    {
        return null;
    }

    @Override
    public int expectedIdempotencyCheckReturnValue()
    {
        return 0;
    }

    @Override
    public void preProcess(Context ctx, Path workDir)
            throws Exception
    {
    }

    @Override
    public void postProcess(Path workDir)
            throws Exception
    {
    }

    @Override
    public void preExecute(Path workDir, List<String> cliArguments)
            throws Exception
    {
    }

    @Override
    public void postExecute(Path workDir)
            throws Exception
    {
    }

    // TODO move into utils?
    // TODO include task inputs?
    protected void interpolateWorkspaceFileAgainstContext(Context ctx, File file)
    {
        try {
            //
            // We need to take the values.yml that is provided and interpolate the content with the
            // Concord context. This allows passing in just-in-time configuration values derived from
            // any Concord operations and also allows passing in secret material from the Concord
            // secrets store or other secrets mechanisms the user may be using.
            //
            if (file.exists()) {
                String fileContent = new String(Files.readAllBytes(file.toPath()));
                if (fileContent.contains("${")) {
                    //
                    // We have interpolation work to do so we will backup the original file to another location
                    // and then created a new interpolated version of the values.yaml in the original location.
                    //
                    File fileOriginal = new File(file + ".original");
                    Files.copy(file.toPath(), fileOriginal.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    String interpolatedFileContent = ctx.eval(fileContent, String.class);
                    Files.write(file.toPath(), interpolatedFileContent.getBytes());
                    logger.info("The {} file was interpolated to the following: \n\n{}", file.getName(), interpolatedFileContent);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
