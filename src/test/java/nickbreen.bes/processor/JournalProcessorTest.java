package nickbreen.bes.processor;

import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import nickbreen.bes.FixturesCompatibilityTest;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static nickbreen.bes.TestUtil.loadBinary;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

public class JournalProcessorTest
{
    @Test
    public void shouldSendEverythingToSink() throws IOException
    {
        final List<OrderedBuildEvent> expected = loadBinary(OrderedBuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/jnl.bin");
        final List<Message> actual = new ArrayList<>();
        final BuildEventProcessor processor = new JournalProcessor(actual::add);
        expected.forEach(processor::accept);

        assertThat("same size", actual, hasSize(expected.size()));
        assertThat("same items", actual, equalTo(expected));
    }

    @Test
    public void shouldSendOneBuildEventWithABazelEventToSink()
    {
        final List<Message> actuals = new ArrayList<>();
        final BuildEventProcessor processor = new JournalProcessor(actuals::add);
        processor.accept(OrderedBuildEvent.newBuilder().setEvent(BuildEvent.newBuilder().setBazelEvent(Any.newBuilder())).build());
        final OrderedBuildEvent expected = OrderedBuildEvent.newBuilder().setEvent(BuildEvent.newBuilder().setBazelEvent(Any.newBuilder()).build()).build();

        assertThat("same size", actuals, hasSize(1));
        assertThat("same items", actuals, hasItem(equalTo(expected)));
    }
}