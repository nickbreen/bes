package kiwi.breen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.util.JsonFormat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonlJournalProcessor extends BuildEventProcessor
{
    private final JsonFormat.Printer printer;
    private final BufferedWriter writer;

    public JsonlJournalProcessor(final JsonFormat.Printer printer, final BufferedWriter writer)
    {
        this.printer = printer;
        this.writer = writer;
    }

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        try
        {
            printer.appendTo(orderedBuildEvent, writer);
            writer.newLine();
            writer.flush();
        }
        catch (final IOException e)
        {
            throw new JournalException("failed to write event to journal", e);
        }
    }

    public static JsonlJournalProcessor create(final BufferedWriter sink)
    {
        final JsonFormat.Printer printer = JsonFormat.printer()
                .usingTypeRegistry(buildTypeRegistry())
                .omittingInsignificantWhitespace();
        return new JsonlJournalProcessor(printer, sink);
    }

    public static JsonlJournalProcessor create(final Path sink)
    {
        try
        {
            return create(Files.newBufferedWriter(sink));
        }
        catch (final IOException e)
        {
            throw new JournalException("failed to create journal", e);
        }
    }

}
