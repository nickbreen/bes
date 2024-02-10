package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEventProto;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import nickbreen.bes.data.EventDAO;
import nickbreen.bes.processor.BazelBuildEventProcessor;
import nickbreen.bes.processor.BuildEventSinkProcessor;
import nickbreen.bes.processor.DatabaseEventProcessor;
import nickbreen.bes.processor.PublishEventProcessor;
import nickbreen.bes.sink.BinaryWriter;
import nickbreen.bes.sink.JsonlWriter;
import nickbreen.bes.sink.TextWriter;

import java.io.PrintWriter;
import java.net.URI;

import static nickbreen.bes.DataSourceFactory.buildDataSource;
import static nickbreen.bes.SinkFactory.createSink;

public interface ProcessorFactory
{
    static PublishEventProcessor create(final URI uri)
    {
        assert uri.isOpaque() : "processor URI's must be opaque";
        final URI sinkUri = URI.create(uri.getSchemeSpecificPart());
        return switch (uri.getScheme())
        {
            case "bazel+text", "bazel" -> new BazelBuildEventProcessor(new TextWriter(buildTextPrinter(), new PrintWriter(createSink(sinkUri), true)));
            case "bazel+json" -> new BazelBuildEventProcessor(new JsonlWriter(buildJsonPrinter(), new PrintWriter(createSink(sinkUri), true)));
            case "bazel+binary" -> new BazelBuildEventProcessor(new BinaryWriter(createSink(sinkUri)));
            case "journal+text" -> new BuildEventSinkProcessor(new TextWriter(buildTextPrinter(), new PrintWriter(createSink(sinkUri), true)));
            case "journal+json" -> new BuildEventSinkProcessor(new JsonlWriter(buildJsonPrinter(), new PrintWriter(createSink(sinkUri), true)));
            case "journal+binary", "journal" -> new BuildEventSinkProcessor(new BinaryWriter(createSink(sinkUri)));
            case "jdbc" -> new DatabaseEventProcessor(new EventDAO(buildDataSource(uri)), buildJsonPrinter());
            default -> throw new Error("Unknown scheme " + uri);
        };
    }

    private static JsonFormat.Printer buildJsonPrinter()
    {
        return JsonFormat.printer().usingTypeRegistry(buildTypeRegistry()).omittingInsignificantWhitespace();
    }

    private static TextFormat.Printer buildTextPrinter()
    {
        return TextFormat.printer().usingTypeRegistry(buildTypeRegistry());
    }

    private static TypeRegistry buildTypeRegistry()
    {
        return TypeRegistry.newBuilder()
                .add(BuildEventProto.getDescriptor().getMessageTypes())
                .add(BuildEventStreamProtos.getDescriptor().getMessageTypes())
                .build();
    }

}
