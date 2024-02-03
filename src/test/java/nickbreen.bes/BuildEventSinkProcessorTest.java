package nickbreen.bes;

import com.google.devtools.build.v1.BuildEvent;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static nickbreen.bes.Util.loadBinary;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BuildEventSinkProcessorTest
{
    @Test
    public void shouldSendEverythingToSink() throws IOException
    {
        final List<BuildEvent> expected = loadBinary(BuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/jnl.bin");
        final List<Message> actual = new ArrayList<>();
        final BuildEventProcessor processor = new BuildEventSinkProcessor(actual::add);
        expected.forEach(processor::accept);

        assertThat("same size", actual, hasSize(expected.size()));
        assertThat("same items", actual, equalTo(expected));
    }

    @Test
    public void shouldSendOneBuildEventWithABazelEventToSink()
    {
        final List<Message> actuals = new ArrayList<>();
        final BuildEventProcessor processor = new BuildEventSinkProcessor(actuals::add);
        processor.accept(BuildEvent.newBuilder().setBazelEvent(Any.newBuilder()).build());
        final BuildEvent expected = BuildEvent.newBuilder().setBazelEvent(Any.newBuilder()).build();

        assertThat("same size", actuals, hasSize(1));
        assertThat("same items", actuals, hasItem(equalTo(expected)));
    }
}