package kiwi.breen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static java.nio.file.Files.newOutputStream;

public class BinaryJournalProcessor extends BuildEventProcessor
{
    private final OutputStream sink;

    protected BinaryJournalProcessor(final OutputStream sink)
    {
        this.sink = sink;
    }

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        try
        {
            orderedBuildEvent.writeDelimitedTo(sink);
        }
        catch (final IOException e)
        {
            throw new JournalException("failed to write event to journal", e);
        }
    }

    public static BinaryJournalProcessor create(final OutputStream sink)
    {
        return new BinaryJournalProcessor(sink);
    }

    public static BinaryJournalProcessor create(final Path sink)
    {
        try
        {
            return create(newOutputStream(sink));
        }
        catch (final IOException e)
        {
            throw new JournalException("failed to create journal", e);
        }
    }
}
