package nickbreen.bes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.OutputStream;
import java.net.URI;

interface SinkFactory
{
    static OutputStream createSink(final URI uri)
    {
        assert uri.isAbsolute() : "sink URI's must have a scheme";
        return switch (uri.getScheme())
        {
            case "file" -> createFile(uri);
            default -> throw new Error("Unknown scheme " + uri);
        };
    }

    private static OutputStream createFile(final URI uri)
    {
        try
        {
            return new FileOutputStream(new File(uri), true);
        }
        catch (FileNotFoundException e)
        {
            throw new IOError(e);
        }
    }
}
