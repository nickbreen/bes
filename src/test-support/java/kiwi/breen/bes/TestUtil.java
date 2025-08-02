package kiwi.breen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.util.JsonFormat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface TestUtil
{
    static <T> List<T> loadBinary(final Util.ParseDelimitedFrom<T> parseDelimitedFrom, final Function<String, InputStream> loader, final String name) throws IOException
    {
        try (final InputStream bes = loader.apply(name))
        {
            return Util.parseBinary(parseDelimitedFrom, bes).toList();
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

}
