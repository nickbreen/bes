package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.*;

public interface Util
{
    static <T> void testAndConsume(final BooleanSupplier test, final Supplier<T> supply, final Consumer<T> consume)
    {
        if (test.getAsBoolean())
        {
            consume.accept(supply.get());
        }
    }

    static <T extends Message> Optional<T> unpack(final Class<T> type, final Any any)
    {
        try
        {
            if (any.is(type))
            {
                return Optional.of(any.unpack(type));
            }
        } catch (final InvalidProtocolBufferException e)
        {
            throw new Error(e);
        }
        return Optional.empty();
    }

    static <T extends Message> void unpackAndConsume(final Class<T> type, final Any any, final Consumer<T> consume)
    {

        try
        {
            if (any.is(type))
            {
                consume.accept(any.unpack(type));
            }
        } catch (final InvalidProtocolBufferException e)
        {
            throw new Error(e);
        }
    }

    static <T> List<T> loadBinary(final ParseDelimitedFrom<T> parseDelimitedFrom, final Function<String, InputStream> loader, final String name) throws IOException
    {
        final List<T> events = new ArrayList<>();
        try (final InputStream bes = loader.apply(name))
        {
            for (T message = parseDelimitedFrom.parseDelimitedFrom(bes); null != message; message = parseDelimitedFrom.parseDelimitedFrom(bes))
            {
                events.add(message);
            }
        }
        return events;
    }

    static List<BuildEventStreamProtos.BuildEvent> loadJsonl(final BuildEventStreamProtos.BuildEvent.Builder builder, final Function<String, InputStream> loader, final String name) throws IOException
    {
        final List<BuildEventStreamProtos.BuildEvent> events = new ArrayList<>();
        final JsonFormat.Parser parser = JsonFormat.parser();
        try (final BufferedReader r = new BufferedReader(new InputStreamReader(loader.apply(name))))
        {
            for (String json = r.readLine(); null != json; json = r.readLine())
            {
                parser.merge(json, builder.clear());
                events.add(builder.build());
            }
        }
        return events;
    }

    @FunctionalInterface
    interface ParseDelimitedFrom<T>
    {
        T parseDelimitedFrom(InputStream is) throws IOException;
    }

    class MessageMatcher<T extends Message, U> extends TypeSafeMatcher<T>
    {
        private final Predicate<T> filter;
        private final Function<T, U> transform;
        private final Matcher<U> matcher;

        public MessageMatcher(final Class<T> t, final Function<T, U> transform, final Matcher<U> matcher)
        {
            this(t, o -> true, transform, matcher);

        }

        public MessageMatcher(final Class<T> t, final Predicate<T> filter, final Function<T, U> transform, final Matcher<U> matcher)
        {
            super(t);
            this.filter = filter;
            this.transform = transform;
            this.matcher = matcher;
        }

        @Override
        protected boolean matchesSafely(final T t)
        {
            return filter.test(t) && matcher.matches(transform.apply(t));
        }

        @Override
        public void describeTo(final Description description)
        {
            description.appendText(" and that ").appendDescriptionOf(matcher);
        }
    }

    class AnyMatcher<T extends Message, U extends Message> extends TypeSafeMatcher<T>
    {
        private final Class<U> any;
        private final Predicate<T> filter;
        private final Function<T, Any> transform;
        private final Matcher<U> matcher;

        public AnyMatcher(final Class<T> t, final Class<U> any, final Predicate<T> filter, final Function<T, Any> transform, final Matcher<U> matcher)
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
            return filter.test(t) && unpack(this.any, transform.apply(t)).map(matcher::matches).orElse(false);
        }

        @Override
        public void describeTo(final Description description)
        {
            description.appendText(" with an Any of type ").appendValue(any.getCanonicalName()).appendText(" that ").appendDescriptionOf(matcher);
        }
    }
}
