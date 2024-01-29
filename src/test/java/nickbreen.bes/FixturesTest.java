package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

public class FixturesTest
{
    private final String expectedId = "f6a5e727-0032-4a84-a959-61e4ecb294e9";

    @Test
    public void shouldReadAllJsonEventsAsJsonObjects()
    {
        assertTrue(Json.createParser(FixturesTest.class.getResourceAsStream("/bes.jsonl")).getValueStream().allMatch(JsonObject.class::isInstance));
    }

    @Test
    public void shouldIdentifyBuildInvocationId() throws IOException
    {
        final InputStream bes = FixturesTest.class.getResourceAsStream("/bes.bin");

        final ArrayList<BuildEventStreamProtos.BuildEvent> events = new ArrayList<>();

        for (BuildEventStreamProtos.BuildEvent message = BuildEventStreamProtos.BuildEvent.parseDelimitedFrom(bes); null != message; message = BuildEventStreamProtos.BuildEvent.parseDelimitedFrom(bes))
        {
            events.add(message);
        }

        assertThat(events, hasSize(33));
        assertThat(events, everyItem(notNullValue()));
    }


}
