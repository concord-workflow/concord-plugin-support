package ca.vanzyl.concord.plugins.tool;

import com.walmartlabs.concord.sdk.Context;

import java.nio.file.Path;
import java.util.List;

public interface ToolCommand {

    String idempotencyCheckCommand(Context context);

    int expectedIdempotencyCheckReturnValue();

    @Deprecated
    void preProcess(Path workDir, Context context) throws Exception;

    @Deprecated
    void postProcess(Path workDir, Context context) throws Exception;

    void preExecute(Context context, Path workDir, List<String> cliArguments) throws Exception;

    void postExecute(Context context, Path workDir) throws Exception;
}