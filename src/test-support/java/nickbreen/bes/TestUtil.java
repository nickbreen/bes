package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
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
}
