package kiwi.breen.bes;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import kiwi.breen.bes.processor.BuildEventProcessor;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static kiwi.breen.bes.TestUtil.loadBinary;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class AcceptanceTest
{
    @Test
    public void shouldWriteJournalEquivalentToSource() throws IOException
    {
        final List<Message> sink = new ArrayList<>();
        final BindableService service = new PublishBuildEventProcessor(
                List.of(new BuildEventProcessor()
                {
                    @Override
                    protected void accept(final OrderedBuildEvent orderedBuildEvent)
                    {
                        sink.add(orderedBuildEvent);
                    }
                }));
        final Thread serverThread = new Thread(new BesServer(8888, service));
        serverThread.start();

        final List<OrderedBuildEvent> events = loadBinary(
                OrderedBuildEvent::parseDelimitedFrom,
                AcceptanceTest.class::getResourceAsStream,
                "/jnl.bin");
        BesClient.create(URI.create("grpc://localhost:8888"))
                .accept(events.stream());

        assertThat(
                sink,
                hasSize(events.size()));
        assertThat(
                sink.stream()
                        .map(OrderedBuildEvent.class::cast)
                        .toList(),
                equalTo(events));

        serverThread.interrupt();
    }
}
