package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.List;

import static nickbreen.bes.AnyMatcher.anyThat;
import static nickbreen.bes.MessageMatcher.messageThat;
import static nickbreen.bes.Util.loadBinary;
import static nickbreen.bes.Util.loadJsonl;
import static nickbreen.bes.Util.unpack;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
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
        assertThat(events, everyItem(notNullValue()));
    }

    @Test
    public void shouldReadAllBinaryEventsAsBazelBuildEvents() throws IOException
    {
        final List<BuildEventStreamProtos.BuildEvent> events = loadBinary(BuildEventStreamProtos.BuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/bes.bin");

        assertThat(events, hasSize(33));
        assertThat(events, everyItem(notNullValue()));
    }

    @Test
    public void shouldReadBinaryJournalAsOrderedBuildEvents() throws IOException
    {
        final List<BuildEvent> events = loadBinary(BuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/jnl.bin");

        assertThat(events, hasSize(32));
        assertThat(events, everyItem(notNullValue()));
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
    public void shouldIdentifyInvocationFromThirdJournalBuildEvent() throws IOException
    {
        final List<BuildEvent> events = loadBinary(BuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/jnl.bin");

        assertThat("more then two events", events, hasSize(greaterThan(2)));
        final BuildEvent first = events.get(2);
        assertThat("hasBazelEvent", first.hasBazelEvent());
        final BuildEventStreamProtos.BuildEvent buildEvent = unpack(BuildEventStreamProtos.BuildEvent.class, first.getBazelEvent()).orElseThrow();
        assertThat("hasStarted", buildEvent.hasStarted());
        assertThat("has invocation id", buildEvent.getStarted().getUuid(), is("c1296c8a-61d6-4fd7-b8c0-42b63e0af62d"));
    }

    @Test
    public void shouldIdentifyInvocationFromAnyJournalBuildEvent() throws IOException
    {
        final List<BuildEvent> events = loadBinary(BuildEvent::parseDelimitedFrom, FixturesCompatibilityTest.class::getResourceAsStream, "/jnl.bin");

        assertThat("an event", events, hasItem(
                anyThat(BuildEvent.class, BuildEventStreamProtos.BuildEvent.class, BuildEvent::hasBazelEvent, BuildEvent::getBazelEvent,
                        messageThat(BuildEventStreamProtos.BuildEvent.class, BuildEventStreamProtos.BuildEvent::hasStarted, BuildEventStreamProtos.BuildEvent::getStarted,
                                messageThat(BuildEventStreamProtos.BuildStarted.class, BuildEventStreamProtos.BuildStarted::getUuid,
                                        is("c1296c8a-61d6-4fd7-b8c0-42b63e0af62d"))))));
    }


}
