package kiwi.breen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.util.JsonFormat;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static kiwi.breen.bes.TestUtil.loadBinary;
import static kiwi.breen.bes.TestUtil.loadJsonl;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonlJournalProcessorTest
{
    private final List<OrderedBuildEvent> events = new ArrayList<>();

    private final JsonFormat.Parser parser = JsonFormat.parser()
            .usingTypeRegistry(BuildEventProcessor.buildTypeRegistry());

    @Before
    public void setUp() throws Exception
    {
        events.addAll(
                loadBinary(
                        OrderedBuildEvent::parseDelimitedFrom,
                        BinaryJournalProcessor.class::getResourceAsStream,
                        "/jnl.bin"));
    }

    @Test
    public void shouldReadAllBinaryEvents()
    {
        assertThat(events).isNotEmpty();
    }

    @Test
    public void shouldReplayIdenticalJournal() throws IOException
    {
        final Path journal = Files.createTempFile("bes", ".jsonl");
        final JsonlJournalProcessor processor = JsonlJournalProcessor.create(journal);
        events.forEach(processor::accept);
        final List<OrderedBuildEvent> events;
        try (final InputStream is = Files.newInputStream(journal))
        {
            events = loadJsonl(
                    OrderedBuildEvent.newBuilder(),
                    parser,
                    ignored -> is,
                    null);
        }
        assertThat(events).isEqualTo(this.events);
    }
}