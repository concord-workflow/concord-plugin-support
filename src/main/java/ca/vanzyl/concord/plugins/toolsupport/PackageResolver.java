package ca.vanzyl.concord.plugins.toolsupport;

import java.net.URI;
import java.nio.file.Path;

public interface PackageResolver
{
    Path resolve(URI uri)
            throws Exception;
}
