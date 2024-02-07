package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.StreamId;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.List;

import static nickbreen.bes.AnyMatcher.anyThat;
import static nickbreen.bes.MessageMatcher.messageThat;
import static nickbreen.bes.TestUtil.loadBinary;
import static nickbreen.bes.TestUtil.loadJsonl;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class FixturesCompatibilityTest
{
    @Test
    public void shouldReadAllJsonEventsAsJsonObjects()
    {
        assertThat("are all objects", Json.createParser(FixturesCompatibilityTest.class.getResourceAsStream("/bes.jsonl")).getValueStream().allMatch(JsonObject.class::isInstance));
    }

    @Test
    public void shouldReadAllJsonEventsAsBazelBuildEvents() throws IOException
    {
        final List<BuildEventStreamProtos.BuildEvent> events = loadJsonl(BuildEventStreamProtos.BuildEvent.newBuilder(), FixturesCompatibilityTest.class::getResourceAsStream, "/bes.jsonl");

        assertThat(events, hasSize(33));
        assertThat(events, everyItem(notNullValue(BuildEventStreamProtos.BuildEvent.class)));
    }

    @Test
    public void shouldReadAllBinaryEventsAsBazelBuildEvents() throws IOException
    {
        final List<BuildEventStreamProtos.BuildEvent> events = loadBinary(BuildEventStreamProtos.BuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/bes.bin");

        assertThat(events, hasSize(33));
        assertThat(events, everyItem(notNullValue(BuildEventStreamProtos.BuildEvent.class)));
    }

    @Test
    public void shouldReadBinaryJournalAsOrderedBuildEvents() throws IOException
    {
        final List<OrderedBuildEvent> events = loadBinary(OrderedBuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/jnl.bin");

        assertThat(events, everyItem(notNullValue(OrderedBuildEvent.class)));
    }

    @Test
    public void shouldIdentifyInvocationFromFirstBuildEvent() throws IOException
    {
        final List<BuildEventStreamProtos.BuildEvent> events = loadBinary(BuildEventStreamProtos.BuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/bes.bin");

        assertThat("not empty", events, not(empty()));
        final BuildEventStreamProtos.BuildEvent first = events.get(0);
        assertThat("hasStarted", first.hasStarted());
        assertThat("has invocation id", first.getStarted().getUuid(), is("f6a5e727-0032-4a84-a959-61e4ecb294e9"));
    }

    @Test
    public void shouldIdentifyInvocationFromStreamIdOfControllerComponent() throws IOException
    {
        final List<OrderedBuildEvent> events = loadBinary(OrderedBuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/jnl.bin");

        assertThat("", events, hasItem(
                messageThat(OrderedBuildEvent.class, OrderedBuildEvent::hasStreamId, OrderedBuildEvent::getStreamId,
                        messageThat(StreamId.class, streamId -> StreamId.BuildComponent.CONTROLLER.equals(streamId.getComponent()), StreamId::getInvocationId, is("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")))));
    }

    @Test
    public void shouldIdentifyInvocationFromAnyJournalBuildEvent() throws IOException
    {
        final List<OrderedBuildEvent> events = loadBinary(OrderedBuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/jnl.bin");

        assertThat("an event", events, hasItem(
                messageThat(OrderedBuildEvent.class, OrderedBuildEvent::hasEvent, OrderedBuildEvent::getEvent,
                        anyThat(BuildEvent.class, BuildEventStreamProtos.BuildEvent.class, BuildEvent::hasBazelEvent, BuildEvent::getBazelEvent,
                                messageThat(BuildEventStreamProtos.BuildEvent.class, BuildEventStreamProtos.BuildEvent::hasStarted, BuildEventStreamProtos.BuildEvent::getStarted,
                                        messageThat(BuildEventStreamProtos.BuildStarted.class, BuildEventStreamProtos.BuildStarted::getUuid,
                                                is("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")))))));
    }


}
