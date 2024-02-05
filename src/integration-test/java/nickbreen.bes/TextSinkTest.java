package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.protobuf.Any;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TextSinkTest
{
    private static final String UUID = "cbf29d13-9dd2-41bd-92dc-06ab2d704990";  // UUID.randomUUID().toString()
    private static final BuildEvent EVENT = BuildEvent.newBuilder()
            .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                    .setStarted(BuildEventStreamProtos.BuildStarted.newBuilder().setUuid(UUID))
                    .build()))
            .build();
    private static final String EXPECTED_TEXT = "bazel_event {\n" +
            "  type_url: \"type.googleapis.com/build_event_stream.BuildEvent\"\n" +
            "  value: \"*&\\n" +
            "$cbf29d13-9dd2-41bd-92dc-06ab2d704990\"\n" +
            "}\n";

    @Test
    public void shouldEncodeAsText() throws IOException
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = new OutputStreamWriter(out);
        new TextSink(writer).accept(EVENT);
        writer.flush();

        final String actualText = out.toString();
        assertThat("to strings are equivalent", actualText, equalTo(EVENT.toString()));
        assertThat("literal to string exact", actualText, equalTo(EXPECTED_TEXT));
    }

    @Test
    public void shouldEncodeAsTextAndWriteToFile() throws IOException
    {
        final Path tmpPath = Files.createTempFile("bes", ".txt");
        final File tmpFile = tmpPath.toFile();
        tmpFile.deleteOnExit();
        final Writer writer = new OutputStreamWriter(new FileOutputStream(tmpFile));

        new TextSink(writer).accept(EVENT);
        writer.flush();

        final String actualText = Files.readString(tmpPath);
        assertThat("text is equivalent", actualText, equalTo(EXPECTED_TEXT));
    }
}
