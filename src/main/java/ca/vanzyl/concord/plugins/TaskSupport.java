package ca.vanzyl.concord.plugins;

import com.walmartlabs.concord.ApiClient;
import com.walmartlabs.concord.client.ApiClientConfiguration;
import com.walmartlabs.concord.client.ApiClientFactory;
import com.walmartlabs.concord.sdk.Context;
import com.walmartlabs.concord.sdk.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public abstract class TaskSupport
        implements Task
{

    protected Map<String, Object> projectInfo(Context context)
    {
        return (Map<String, Object>) context.getVariable("projectInfo");
    }

    protected String orgName(Context context)
    {
        return (String) projectInfo(context).get("orgName");
    }

    protected Path workDirPath(Context context, String path)
    {
        return workDir(context).resolve(path);
    }

    protected Path workDir(Context context)
    {
        Path workDir = Paths.get((String) context.getVariable(com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY));
        if (workDir == null) {
            throw new IllegalArgumentException("Can't determine the current '" + com.walmartlabs.concord.sdk.Constants.Context.WORK_DIR_KEY + "'");
        }
        return workDir;
    }

    protected String varAsString(Context context, String variableName)
    {
        return (String) context.getVariable(variableName);
    }

    protected boolean varAsBoolean(Context context, String variableName)
    {
        Object booleanValue = context.getVariable(variableName);
        if (booleanValue == null) {
            return false;
        }
        else if (booleanValue.getClass().isAssignableFrom(Boolean.class)) {
            // debug: true/false
            return (Boolean) booleanValue;
        }
        else {
            // debug: "true/false"
            return Boolean.parseBoolean((String) context.getVariable(variableName));
        }
    }

    protected Map<String, Object> varAsMap(Context context, String variableName)
    {
        return (Map<String, Object>) context.getVariable(variableName);
    }

    protected Map<String, String> varAsStringMap(Context context, String variableName)
    {
        return (Map<String, String>) context.getVariable(variableName);
    }

    protected String varAsString(Map<String, Object> context, String variableName)
    {
        return (String) context.get(variableName);
    }

    // ----------------------------------------------------------------------------------------------------
    // Temporary files/dirs
    // ----------------------------------------------------------------------------------------------------

    protected static Path createTmpFile(Path workDir, String prefix, String suffix)
            throws IOException
    {
        return Files.createTempFile(prefix, suffix);
        /*
        Path tmpDir = workDir.resolve(Constants.Files.CONCORD_SYSTEM_DIR_NAME);
        if (!Files.exists(tmpDir)) {
            Files.createDirectories(tmpDir);
        }
        return Files.createTempFile(tmpDir, prefix, suffix);

         */
    }

    // ----------------------------------------------------------------------------------------------------
    // API client
    // ----------------------------------------------------------------------------------------------------

    protected static ApiClient apiClient(ApiClientFactory apiClientFactory, Context context)
    {
        return apiClientFactory
                .create(ApiClientConfiguration.builder()
                        .context(context)
                        .build());
    }
}
