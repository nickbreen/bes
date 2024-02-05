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
    static Consumer<Message> createSink(final URI uri, final BiConsumer<Message, OutputStream> writer)
    {
        assert uri.isAbsolute() : "sink URI's must have a scheme";
        return switch (uri.getScheme())
        {
            case "file":
                yield createFile(uri, writer);
            case "jdbc":
                yield createJdbc(uri);
            case "redis":
                yield createRedis(uri);
            default:
                throw new Error("Unknown scheme " + uri);
        };
    }

    private static Consumer<Message> createJdbc(final URI uri)
    {
        throw new Error("JDBC not supported yet " + uri);
    }

    private static Consumer<Message> createRedis(final URI uri)
    {
        throw new Error("Redis not supported yet " + uri);
    }

    private static Consumer<Message> createFile(final URI uri, final BiConsumer<Message, OutputStream> writer)
    {
        try
        {
            @SuppressWarnings("resource") // it is used in the lambda, so we cannot try-with-resources
            final OutputStream outputStream = new FileOutputStream(new File(uri));
            return message -> writer.accept(message, outputStream);
        }
        catch (FileNotFoundException e)
        {
            throw new IOError(e);
        }
    }
}
