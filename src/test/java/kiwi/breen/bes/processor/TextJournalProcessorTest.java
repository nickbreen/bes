package kiwi.breen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static kiwi.breen.bes.TestUtil.loadBinary;
import static org.assertj.core.api.Assertions.assertThat;

public class TextJournalProcessorTest
{
    private final List<OrderedBuildEvent> events = new ArrayList<>();

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
    public void shouldGenerateIdenticalTextJournal() throws IOException
    {
        final Path journal = Files.createTempFile("bes", ".txt");
        final TextJournalProcessor processor = TextJournalProcessor.create(journal);
        events.forEach(processor::accept);
        final String actual = Files.readString(journal);
        try (final InputStream is = TextJournalProcessorTest.class.getResourceAsStream("/jnl.text"))
        {
            final String expected = new String(is.readAllBytes());
            assertThat(actual).isEqualTo(expected);
        }
    }
}