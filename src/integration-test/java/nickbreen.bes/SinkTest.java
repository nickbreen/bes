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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.protobuf.TypeRegistry.newBuilder;
import static java.util.Arrays.copyOfRange;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SinkTest
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
    private static final String EXPECTED_JSON = "{\"bazelEvent\":{\"@type\":\"type.googleapis.com/build_event_stream.BuildEvent\",\"started\":{\"uuid\":\"cbf29d13-9dd2-41bd-92dc-06ab2d704990\"}}}";
    private static final byte[] EXPECTED_BYTES = new byte[]{
            -30, 3, 93, 10, 49, 116, 121, 112,
            101, 46, 103, 111, 111, 103, 108, 101,
            97, 112, 105, 115, 46, 99, 111, 109,
            47, 98, 117, 105, 108, 100, 95, 101,
            118, 101, 110, 116, 95, 115, 116, 114,
            101, 97, 109, 46, 66, 117, 105, 108,
            100, 69, 118, 101, 110, 116, 18, 40,
            42, 38, 10, 36, 99, 98, 102, 50,
            57, 100, 49, 51, 45, 57, 100, 100,
            50, 45, 52, 49, 98, 100, 45, 57,
            50, 100, 99, 45, 48, 54, 97, 98,
            50, 100, 55, 48, 52, 57, 57, 48};

    private final TypeRegistry typeRegistry = newBuilder().add(BuildEventStreamProtos.getDescriptor().getMessageTypes()).build();
    private final JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry).omittingInsignificantWhitespace();

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
    public void shouldEncodeAsDelimitedBinary() throws IOException
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new BinarySink(out).accept(EVENT);
        out.flush();

        final byte[] delimitedBytes = out.toByteArray();
        assertThat("expected bytes haven't drifted", EXPECTED_BYTES, equalTo(EVENT.toByteArray()));
        assertThat("as Delimited Binary", delimitedBytes.length, equalTo(1 + EXPECTED_BYTES.length));
        final int length = delimitedBytes[0];
        final byte[] actualBytes = copyOfRange(delimitedBytes, 1, 1 + length);
        assertThat("as Binary", actualBytes, equalTo(EXPECTED_BYTES));
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

    @Test
    public void shouldEncodeAsDelimitedBinaryAndWriteToFile() throws IOException
    {
        final Path tmpPath = Files.createTempFile("bes", ".bin");
        final File tmpFile = tmpPath.toFile();
        tmpFile.deleteOnExit();

        final OutputStream out = new FileOutputStream(tmpFile);
        new BinarySink(out).accept(EVENT);
        out.flush();

        final byte[] delimitedBytes = Files.readAllBytes(tmpPath);

        assertThat("as Delimited Binary", delimitedBytes.length, equalTo(1 + EXPECTED_BYTES.length));
        final int length = delimitedBytes[0];
        final byte[] actualBytes = copyOfRange(delimitedBytes, 1, 1 + length);
        assertThat("as Binary", actualBytes, equalTo(EXPECTED_BYTES));
    }

}
