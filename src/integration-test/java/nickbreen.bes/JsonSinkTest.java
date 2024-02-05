package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.protobuf.Any;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.protobuf.TypeRegistry.newBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class JsonSinkTest
{
    private static final String UUID = "cbf29d13-9dd2-41bd-92dc-06ab2d704990";  // UUID.randomUUID().toString()
    private static final BuildEvent EVENT = BuildEvent.newBuilder()
            .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                    .setStarted(BuildEventStreamProtos.BuildStarted.newBuilder().setUuid(UUID))
                    .build()))
            .build();
    private static final String EXPECTED_JSON = "{\"bazelEvent\":{\"@type\":\"type.googleapis.com/build_event_stream.BuildEvent\",\"started\":{\"uuid\":\"cbf29d13-9dd2-41bd-92dc-06ab2d704990\"}}}";

    private final TypeRegistry typeRegistry = newBuilder().add(BuildEventStreamProtos.getDescriptor().getMessageTypes()).build();
    private final JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry).omittingInsignificantWhitespace();

    @Test
    public void shouldEncodeAsJson() throws IOException
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = new OutputStreamWriter(out);
        new JsonSink(printer, writer).accept(EVENT);
        writer.flush();

        final String actualJson = out.toString();
        assertThat("as minimal JSON", actualJson, equalTo(EXPECTED_JSON));
    }

    @Test
    public void shouldEncodeAsJsonAndWriteToFile() throws IOException
    {
        final Path tmpPath = Files.createTempFile("bes", ".json");
        final File tmpFile = tmpPath.toFile();
        tmpFile.deleteOnExit();
        final Writer writer = new OutputStreamWriter(new FileOutputStream(tmpFile));

        new JsonSink(printer, writer).accept(EVENT);
        writer.flush();

        final String actualJson = Files.readString(tmpPath);
        assertThat("JSON is equivalent", actualJson, equalTo(EXPECTED_JSON));
    }

}
