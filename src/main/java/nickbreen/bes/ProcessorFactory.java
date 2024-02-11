package nickbreen.bes;

import nickbreen.bes.data.EventDAO;
import nickbreen.bes.processor.DatabaseProcessor;
import nickbreen.bes.processor.JournalProcessor;
import nickbreen.bes.processor.PublishEventProcessor;
import nickbreen.bes.sink.BinaryWriter;
import nickbreen.bes.sink.JsonlWriter;
import nickbreen.bes.sink.TextWriter;

import java.io.PrintWriter;
import java.net.URI;

import static nickbreen.bes.DataSourceFactory.buildDataSource;
import static nickbreen.bes.SinkFactory.createSink;
import static nickbreen.bes.Util.buildJsonPrinter;
import static nickbreen.bes.Util.buildTextPrinter;

public interface ProcessorFactory
{
    static PublishEventProcessor create(final URI uri)
    {
        assert uri.isOpaque() : "processor URI's must be opaque";
        final URI sinkUri = URI.create(uri.getSchemeSpecificPart());
        return switch (uri.getScheme())
        {
            case "journal+text" -> new JournalProcessor(new TextWriter(buildTextPrinter(), new PrintWriter(createSink(sinkUri), true)));
            case "journal+json" -> new JournalProcessor(new JsonlWriter(buildJsonPrinter(), new PrintWriter(createSink(sinkUri), true)));
            case "journal+binary", "journal" -> new JournalProcessor(new BinaryWriter(createSink(sinkUri)));
            case "jdbc" -> new DatabaseProcessor(new EventDAO(buildDataSource(uri)), buildJsonPrinter());
            default -> throw new Error("Unknown scheme " + uri);
        };
    }
}
