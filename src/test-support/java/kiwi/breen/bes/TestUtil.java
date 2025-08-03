package kiwi.breen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public interface TestUtil
{
    static <T, U> List<T> loadBinary(final ParseDelimitedFrom<T> parseDelimitedFrom, final Function<U, InputStream> loader, final U name) throws IOException
    {
        try (final InputStream bes = loader.apply(name))
        {
            return parseBinary(parseDelimitedFrom, bes).toList();
        }
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

    static List<OrderedBuildEvent> loadJsonl(final OrderedBuildEvent.Builder builder, final JsonFormat.Parser parser, final Function<String, InputStream> loader, final String name) throws IOException
    {
        final List<OrderedBuildEvent> events = new ArrayList<>();
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

    static <T> Stream<T> parseBinary(final ParseDelimitedFrom<T> parseDelimitedFrom, final InputStream bes) throws IOException
    {
        final Stream.Builder<T> events = Stream.builder();
        for (T message = parseDelimitedFrom.parseDelimitedFrom(bes); null != message; message = parseDelimitedFrom.parseDelimitedFrom(bes))
        {
            events.add(message);
        }
        return events.build();
    }

    static List<OrderedBuildEvent> parseDelimitedJson(final OrderedBuildEvent.Builder builder, final JsonFormat.Parser parser, final InputStream bes)
    {
        try (final BufferedReader r = new BufferedReader(new InputStreamReader(bes)))
        {
            return r.lines().map(json -> {
                try
                {
                    parser.merge(json, builder.clear());
                }
                catch (InvalidProtocolBufferException e)
                {
                    throw new Error(e);
                }
                return builder.build();
            }).toList();
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }

    @FunctionalInterface
    interface ParseDelimitedFrom<T>
    {
        T parseDelimitedFrom(InputStream is) throws IOException;
    }
}
