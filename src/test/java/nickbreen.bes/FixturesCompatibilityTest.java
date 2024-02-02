package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.hamcrest.*;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static nickbreen.bes.Util.unpack;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FixturesCompatibilityTest
{

    @Test
    public void shouldReadAllJsonEventsAsJsonObjects()
    {
        assertThat("are all objects", Json.createParser(FixturesCompatibilityTest.class.getResourceAsStream("/bes.jsonl")).getValueStream().allMatch(JsonObject.class::isInstance));
    }

    @Test
    public void shouldReadAllBinaryEventsAsBazelBuildEvents() throws IOException
    {
        final List<BuildEventStreamProtos.BuildEvent> events = loadBinary("/bes.bin", BuildEventStreamProtos.BuildEvent::parseDelimitedFrom);

        assertThat(events, hasSize(33));
        assertThat(events, everyItem(notNullValue()));
    }

    @Test
    public void shouldReadBinaryJournalAsOrderedBuildEvents() throws IOException
    {
        final List<BuildEvent> events = loadBinary("/jnl.bin", BuildEvent::parseDelimitedFrom);

        assertThat(events, hasSize(32));
        assertThat(events, everyItem(notNullValue()));
    }

    @Test
    public void shouldIdentifyInvocationFromFirstBuildEvent() throws IOException
    {
        final List<BuildEventStreamProtos.BuildEvent> events = loadBinary("/bes.bin", BuildEventStreamProtos.BuildEvent::parseDelimitedFrom);

        final BuildEventStreamProtos.BuildEvent first = events.get(0);
        assertThat("hasStarted", first.hasStarted());
        assertThat(first.getStarted().getUuid(), is("f6a5e727-0032-4a84-a959-61e4ecb294e9"));
    }

    @Test
    public void shouldIdentifyInvocationFromThirdJournalBuildEvent() throws IOException
    {
        final List<BuildEvent> events = loadBinary("/jnl.bin", BuildEvent::parseDelimitedFrom);

        final BuildEvent first = events.get(2);
        assertThat("hasBazelEvent", first.hasBazelEvent());
        final BuildEventStreamProtos.BuildEvent buildEvent = unpack(BuildEventStreamProtos.BuildEvent.class, first.getBazelEvent()).orElseThrow();
        assertThat("hasStarted", buildEvent.hasStarted());
        assertThat(buildEvent.getStarted().getUuid(), is("c1296c8a-61d6-4fd7-b8c0-42b63e0af62d"));
    }

    @Test
    public void shouldIdentifyInvocationFromAnyJournalBuildEvent() throws IOException
    {
        final List<BuildEvent> events = loadBinary("/jnl.bin", BuildEvent::parseDelimitedFrom);

        assertThat("an event", events, hasItem(
                new AnyMatcher<>(BuildEvent.class, BuildEventStreamProtos.BuildEvent.class, BuildEvent::hasBazelEvent, BuildEvent::getBazelEvent,
                        new MessageMatcher<>(BuildEventStreamProtos.BuildEvent.class, BuildEventStreamProtos.BuildEvent::hasStarted, BuildEventStreamProtos.BuildEvent::getStarted,
                                new MessageMatcher<>(BuildEventStreamProtos.BuildStarted.class, BuildEventStreamProtos.BuildStarted::getUuid,
                                        is("c1296c8a-61d6-4fd7-b8c0-42b63e0af62d"))))));
    }

    private static class MessageMatcher<T extends Message, U> extends TypeSafeMatcher<T>
    {
        private final Function<T, Boolean> filter;
        private final Function<T, U> transform;
        private final Matcher<U> matcher;

        public MessageMatcher(final Class<T> t, final Function<T, U> transform, final Matcher<U> matcher)
        {
            this(t, o -> true, transform, matcher);
        }

        public MessageMatcher(final Class<T> t, final Function<T, Boolean> filter, final Function<T, U> transform, final Matcher<U> matcher)
        {
            super(t);
            this.filter = filter;
            this.transform = transform;
            this.matcher = matcher;
        }

        @Override
        protected boolean matchesSafely(final T t)
        {
            return filter.apply(t) && matcher.matches(transform.apply(t));
        }

        @Override
        public void describeTo(final Description description)
        {
            description.appendText(" and that ").appendDescriptionOf(matcher);
        }
    }

    private static class AnyMatcher<T extends Message, U extends Message> extends TypeSafeMatcher<T>
    {
        private final Class<U> any;
        private final Function<T, Boolean> filter;
        private final Function<T, Any> transform;
        private final Matcher<U> matcher;

        public AnyMatcher(final Class<T> t, final Class<U> any, final Function<T, Boolean> filter, final Function<T, Any> transform, final Matcher<U> matcher)
        {
            super(t);
            this.any = any;
            this.filter = filter;
            this.transform = transform;
            this.matcher = matcher;
        }

        @Override
        protected boolean matchesSafely(final T t)
        {
            return filter.apply(t) && unpack(this.any, transform.apply(t)).map(matcher::matches).orElse(false);
        }

        @Override
        public void describeTo(final Description description)
        {
            description.appendText(" with an Any of type ").appendValue(any.getCanonicalName()).appendText(" that ").appendDescriptionOf(matcher);
        }
    }


    @FunctionalInterface
    private interface ParseDelimitedFrom<T>
    {
        T parseDelimitedFrom(InputStream is) throws IOException;
    }

    private static <T> List<T> loadBinary(final String name, final ParseDelimitedFrom<T> parseDelimitedFrom) throws IOException
    {
        final List<T> events = new ArrayList<>();
        try (final InputStream bes = FixturesCompatibilityTest.class.getResourceAsStream(name))
        {
            for (T message = parseDelimitedFrom.parseDelimitedFrom(bes); null != message; message = parseDelimitedFrom.parseDelimitedFrom(bes))
            {
                events.add(message);
            }
        }
        return events;
    }

}
