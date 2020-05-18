package ca.vanzyl.concord.plugins.tool;

public enum OS
{
    DARWIN(""),
    LINUX(""),
    WINDOWS(".exe"),
    SOLARIS("");

    static OS CURRENT = resolveOs();

    private String osName;
    private String executableSuffix;

    OS(String executableSuffix)
    {
        this.osName = name().toLowerCase();
        this.executableSuffix = executableSuffix;
    }

    public String appendExecutableSuffix(String executableName){
        return String.join("", executableName, executableSuffix);
    }

    public String getOsName()
    {
        return this.osName;
    }

    private static OS resolveOs()
    {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("mac") >= 0) {
            return DARWIN;
        }
        else if (os.indexOf("nux") >= 0) {
            return LINUX;
        }
        else if (os.indexOf("win") >= 0) {
            return WINDOWS;
        }
        else if (os.indexOf("sunos") >= 0) {
            return SOLARIS;
        }
        else {
            throw new IllegalArgumentException("Your operating system is not supported: " + os);
        }
    }
}
