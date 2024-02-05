package nickbreen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEventProto;
import com.google.protobuf.Message;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import nickbreen.bes.sink.SinkFactory;

import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public interface ProcessorFactory
{
    static PublishEventProcessor create(final URI uri)
    {
        assert uri.isOpaque() : "processor URI's must be opaque";
        final URI sinkUri = URI.create(uri.getSchemeSpecificPart());
        return switch (uri.getScheme())
        {
            case "bazel+text", "bazel" -> new BazelBuildEventProcessor(SinkFactory.createSink(sinkUri, new TextWriter()));
            case "bazel+json" -> new BazelBuildEventProcessor(SinkFactory.createSink(sinkUri, new JsonlWriter(buildPrinter())));
            case "bazel+binary" -> new BazelBuildEventProcessor(SinkFactory.createSink(sinkUri, new BinaryWriter()));
            case "journal+text" -> new BuildEventSinkProcessor(SinkFactory.createSink(sinkUri, new TextWriter()));
            case "journal+json" -> new BuildEventSinkProcessor(SinkFactory.createSink(sinkUri, new JsonlWriter(buildPrinter())));
            case "journal+binary", "journal" -> new BuildEventSinkProcessor(SinkFactory.createSink(sinkUri, new BinaryWriter()));
            default -> throw new Error("Unknown scheme " + uri);
        };
    }

    private static JsonFormat.Printer buildPrinter()
    {
        final TypeRegistry typeRegistry = TypeRegistry.newBuilder()
                .add(BuildEventProto.getDescriptor().getMessageTypes())
                .add(BuildEventStreamProtos.getDescriptor().getMessageTypes())
                .build();
        return JsonFormat.printer().usingTypeRegistry(typeRegistry).omittingInsignificantWhitespace();
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
                outputStream.write('\n');
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
