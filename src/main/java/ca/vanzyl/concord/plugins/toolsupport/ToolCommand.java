package ca.vanzyl.concord.plugins.toolsupport;

import com.walmartlabs.concord.runtime.v2.sdk.Context;

import java.nio.file.Path;
import java.util.List;

public interface ToolCommand
{
    String idempotencyCheckCommand(Context ctx);

    int expectedIdempotencyCheckReturnValue();

    @Deprecated
    void preProcess(Context ctx, Path workDir)
            throws Exception;

    @Deprecated
    void postProcess(Path workDir)
            throws Exception;

    void preExecute(Path workDir, List<String> cliArguments)
            throws Exception;

    void postExecute(Path workDir)
            throws Exception;
}
