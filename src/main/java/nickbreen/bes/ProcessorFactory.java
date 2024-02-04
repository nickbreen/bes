package nickbreen.bes;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ProcessorFactory
{
    static PublishEventProcessor create(final URI uri)
    {
        assert uri.isOpaque() : "processor URI's must be opaque";
        final URI sinkUri = URI.create(uri.getSchemeSpecificPart());
        final BiConsumer<Message, OutputStream> writer = createWriter(sinkUri);
        return switch (uri.getScheme())
        {
            case "bazel" -> new BazelBuildEventProcessor(SinkFactory.createSink(sinkUri, writer));
            case "journal" -> new BuildEventSinkProcessor(SinkFactory.createSink(sinkUri, writer));
            default -> throw new Error("Unknown scheme " + uri);
        };
    }

    private static BiConsumer<Message, OutputStream> createWriter(final URI uri)
    {
        final Path fileName = Path.of(uri).getFileName();
//        conditional switch pattern matching would be nice
//        return switch (fileName)
//        {
//            case Path p && p.endsWith(".bin") -> new BinaryWriter();
//            case Path p && p.endsWith(".jsonl") -> new JsonlWriter(JsonFormat.printer());
//            default -> new TextWriter();
//        }
        if (fileName.endsWith(".bin"))
        {
            return new BinaryWriter();
        }
        if (fileName.endsWith(".jsonl") || fileName.endsWith(".json"))
        {
            return new JsonlWriter(JsonFormat.printer());
        }
        return new TextWriter();
    }

    class JsonlWriter implements BiConsumer<Message, OutputStream>
    {
        private final JsonFormat.Printer printer;

        public JsonlWriter(final JsonFormat.Printer printer)
        {

            this.printer = printer;
        }

        @Override
        public void accept(final Message message, final OutputStream outputStream)
        {
            try
            {
                outputStream.write(printer.print(message).getBytes());
            }
            catch (IOException e)
            {
                throw new IOError(e);
            }
        }
    }

    class TextWriter implements BiConsumer<Message, OutputStream>
    {
        @Override
        public void accept(final Message message, final OutputStream outputStream)
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
    }

    class BinaryWriter implements BiConsumer<Message, OutputStream>
    {
        @Override
        public void accept(final Message message, final OutputStream outputStream)
        {
            try
            {
                message.writeDelimitedTo(outputStream);
            }
            catch (IOException e)
            {
                throw new IOError(e);
            }
        }
    }
}
