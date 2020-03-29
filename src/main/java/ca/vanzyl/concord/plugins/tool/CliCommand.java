package ca.vanzyl.concord.plugins.tool;

import com.google.common.collect.ImmutableMap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CliCommand
{
    private Path workDir;
    private List<String> args;
    private Map<String, String> envars;
    private final boolean saveOutput;

    public CliCommand(String args, Path workDir)
            throws Exception
    {
        this(Arrays.asList(args.split(" ")), workDir, ImmutableMap.of(), false);
    }

    public CliCommand(String args, Path workDir, Map<String,String> envars, boolean saveOutput)
            throws Exception
    {
        this(Arrays.asList(args.split(" ")), workDir, envars, saveOutput);
    }

    public CliCommand(List<String> args, Path workDir, Map<String, String> envars, boolean saveOutput)
            throws Exception
    {
        this.workDir = workDir;
        this.args = args;
        this.envars = envars;
        this.saveOutput = saveOutput;
    }

    public Result execute()
            throws Exception
    {
        return execute(Executors.newCachedThreadPool());
    }

    public Result execute(ExecutorService executor)
            throws Exception
    {
        ProcessBuilder pb = new ProcessBuilder(args).directory(workDir.toFile());
        Map<String, String> combinedEnv = new HashMap<>(envars);
        pb.environment().putAll(combinedEnv);
        Process p = pb.start();

        Future<String> stderr = executor.submit(new StreamReader(saveOutput, p.getErrorStream()));
        Future<String> stdout = executor.submit(new StreamReader(saveOutput, p.getInputStream()));

        int code = p.waitFor();
        return new Result(code, stdout.get(), stderr.get());
    }

    public List<String> getCommand()
    {
        return args;
    }

    protected static void log(String line)
    {
        System.out.println(line);
    }

    private static class StreamReader
            implements Callable<String>
    {
        private final boolean saveOutput;
        private final InputStream in;

        private StreamReader(boolean saveOutput, InputStream in)
        {
            this.saveOutput = saveOutput;
            this.in = in;
        }

        @Override
        public String call()
                throws Exception
        {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (saveOutput) {
                        sb.append(line).append(System.lineSeparator());
                    }
                    log(line);
                }
            }
            return sb.toString();
        }
    }

    public static class Result
    {
        private final int code;
        private final String stdout;
        private final String stderr;

        public Result(int code, String stdout, String stderr)
        {
            this.code = code;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int getCode()
        {
            return code;
        }

        public String getStdout()
        {
            return stdout;
        }

        public String getStderr()
        {
            return stderr;
        }
    }
}