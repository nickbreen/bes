package nickbreen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEventProto;
import com.google.protobuf.Message;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import nickbreen.bes.DataSourceFactory;
import nickbreen.bes.sink.SinkFactory;

import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.function.Consumer;

public interface ProcessorFactory
{
    static PublishEventProcessor create(final URI uri)
    {
        assert uri.isOpaque() : "processor URI's must be opaque";
        final URI sinkUri = URI.create(uri.getSchemeSpecificPart());
        final OutputStream sink = SinkFactory.createSink(sinkUri);
        return switch (uri.getScheme())
        {
            case "bazel+text", "bazel" -> new BazelBuildEventProcessor(new TextWriter(new PrintWriter(sink)));
            case "bazel+json" -> new BazelBuildEventProcessor(new JsonlWriter(buildPrinter(), new PrintWriter(sink)));
            case "bazel+binary" -> new BazelBuildEventProcessor(new BinaryWriter(sink));
            case "journal+text" -> new BuildEventSinkProcessor(new TextWriter(new PrintWriter(sink)));
            case "journal+json" -> new BuildEventSinkProcessor(new JsonlWriter(buildPrinter(), new PrintWriter(sink)));
            case "journal+binary", "journal" -> new BuildEventSinkProcessor(new BinaryWriter(sink));
            case "jdbc" -> new DatabaseEventProcessor(DataSourceFactory.buildDataSource(uri), buildPrinter());
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

    class JsonlWriter implements Consumer<Message>
    {
        private final JsonFormat.Printer printer;
        private final PrintWriter sink;

        public JsonlWriter(final JsonFormat.Printer printer, final PrintWriter sink)
        {
            this.printer = printer;
            this.sink = sink;
        }

        @Override
        public void accept(final Message message)
        {
            try
            {
                printer.appendTo(message, sink);
                sink.println();
            }
            catch (IOException e)
            {
                throw new IOError(e);
            }
        }
    }

    class TextWriter implements Consumer<Message>
    {
        private final PrintWriter sink;

        public TextWriter(final PrintWriter sink)
        {
            this.sink = sink;
        }

        @Override
        public void accept(final Message message)
        {
            sink.println(message.toString());
        }
    }

    class BinaryWriter implements Consumer<Message>
    {
        private final OutputStream sink;

        public BinaryWriter(final OutputStream sink)
        {
            this.sink = sink;
        }

        @Override
        public void accept(final Message message)
        {
            try
            {
                message.writeDelimitedTo(sink);
            }
            catch (IOException e)
            {
                throw new IOError(e);
            }
        }
    }
}
