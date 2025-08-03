package kiwi.breen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.TextFormat;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextJournalProcessor extends BuildEventProcessor
{
    private final TextFormat.Printer printer;
    private final Writer writer;

    public TextJournalProcessor(final TextFormat.Printer printer, final Writer writer)
    {
        this.printer = printer;
        this.writer = writer;
    }

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        try
        {
            printer.print(orderedBuildEvent, writer);
            writer.flush();
        }
        catch (final IOException e)
        {
            throw new JournalException("failed to write event to journal", e);
        }
    }

    public static TextJournalProcessor create(final Writer sink)
    {
        final TextFormat.Printer textPrinter = TextFormat.printer().usingTypeRegistry(buildTypeRegistry());
        return new TextJournalProcessor(textPrinter, sink);
    }

    public static TextJournalProcessor create(final Path sink)
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
