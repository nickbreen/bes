package nickbreen.bes;

import com.google.protobuf.Message;

import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.function.Consumer;

public interface ProcessorFactory
{
    static PublishEventProcessor create(final URI uri)
    {
        assert uri.isAbsolute() : "processor URI's must have a scheme";
        assert uri.isOpaque() : "processor URI's must be opaque";
        final URI sinkUri = URI.create(uri.getSchemeSpecificPart());
        return switch (uri.getScheme())
        {
            case "bazel" -> new BazelBuildEventProcessor(SinkFactory.createSink(sinkUri, ProcessorFactory::writeText));
            case "journal" -> new BuildEventSinkProcessor(SinkFactory.createSink(sinkUri, ProcessorFactory::writeBinary));
            default -> throw new Error("Unknown scheme " + uri);
        };
    }

    private static void writeText(final Message message, final OutputStream outputStream)
    {
        try
        {
            outputStream.write(message.toString().getBytes());
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }

    private static void writeBinary(final Message message, final OutputStream outputStream)
    {
        try
        {
            message.writeDelimitedTo(outputStream);
        }
        catch (final IOException e)
        {
            throw new IOError(e);
        }
    }
}
