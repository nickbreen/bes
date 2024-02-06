package nickbreen.bes.sink;

import com.google.protobuf.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.OutputStream;
import java.net.URI;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface SinkFactory
{
    static OutputStream createSink(final URI uri)
    {
        assert uri.isAbsolute() : "sink URI's must have a scheme";
        return switch (uri.getScheme())
        {
            case "file":
                yield createFile(uri);
            case "jdbc":
                yield createJdbc(uri);
            case "redis":
                yield createRedis(uri);
            default:
                throw new Error("Unknown scheme " + uri);
        };
    }

    private static OutputStream createJdbc(final URI uri)
    {
        throw new Error("JDBC not supported yet " + uri);
    }

    private static OutputStream createRedis(final URI uri)
    {
        throw new Error("Redis not supported yet " + uri);
    }

    private static OutputStream createFile(final URI uri)
    {
        try
        {
            return new FileOutputStream(new File(uri));
        }
        catch (FileNotFoundException e)
        {
            throw new IOError(e);
        }
    }
}
