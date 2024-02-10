package nickbreen.bes.sink;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.protobuf.Any;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TypeRegistry;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.protobuf.TypeRegistry.newBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TextSinkIntegrationTest
{
    private static final String UUID = "cbf29d13-9dd2-41bd-92dc-06ab2d704990";  // UUID.randomUUID().toString()
    private static final BuildEvent EVENT = BuildEvent.newBuilder()
            .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                    .setStarted(BuildEventStreamProtos.BuildStarted.newBuilder().setUuid(UUID))
                    .build()))
            .build();
    private static final String EXPECTED_TEXT = "bazel_event {\n  [type.googleapis.com/build_event_stream.BuildEvent] {\n    started {\n      uuid: \"cbf29d13-9dd2-41bd-92dc-06ab2d704990\"\n    }\n  }\n}\n\n";

    private final TypeRegistry typeRegistry = newBuilder().add(BuildEventStreamProtos.getDescriptor().getMessageTypes()).build();
    private final TextFormat.Printer printer = TextFormat.printer().usingTypeRegistry(typeRegistry);

    @Test
    public void shouldEncodeAsText()
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PrintWriter writer = new PrintWriter(out);
        new TextWriter(printer, writer).accept(EVENT);
        writer.flush();

        final String actualText = out.toString();
        assertThat("literal to string exact", actualText, equalTo(EXPECTED_TEXT));
    }

    @Test
    public void shouldEncodeAsTextAndWriteToFile() throws IOException
    {
        final Path tmpPath = Files.createTempFile("bes", ".txt");
        final File tmpFile = tmpPath.toFile();
        tmpFile.deleteOnExit();
        final PrintWriter writer = new PrintWriter(new FileOutputStream(tmpFile));

        new TextWriter(printer, writer).accept(EVENT);
        writer.flush();

        final String actualText = Files.readString(tmpPath);
        assertThat("text is equivalent", actualText, equalTo(EXPECTED_TEXT));
    }
}
