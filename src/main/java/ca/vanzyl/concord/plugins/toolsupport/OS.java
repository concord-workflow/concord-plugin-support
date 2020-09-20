package ca.vanzyl.concord.plugins.toolsupport;

public enum OS
{
    DARWIN(""),
    LINUX(""),
    WINDOWS(".exe"),
    SOLARIS("");

    public static OS CURRENT = resolveOs();

    private final String osName;
    private final String executableSuffix;

    OS(String executableSuffix)
    {
        this.osName = name().toLowerCase();
        this.executableSuffix = executableSuffix;
    }

    public String appendExecutableSuffix(String executableName)
    {
        return String.join("", executableName, executableSuffix);
    }

    public String getOsName()
    {
        return this.osName;
    }

    private static OS resolveOs()
    {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return DARWIN;
        }
        else if (os.contains("nux")) {
            return LINUX;
        }
        else if (os.contains("win")) {
            return WINDOWS;
        }
        else if (os.contains("sunos")) {
            return SOLARIS;
        }
        else {
            throw new IllegalArgumentException("Your operating system is not supported: " + os);
        }
    }
}
