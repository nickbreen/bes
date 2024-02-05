package nickbreen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.BuildEventProto;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.StreamId;
import com.google.protobuf.Any;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import nickbreen.bes.processor.BuildEventSinkProcessor;
import nickbreen.bes.sink.JsonSink;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.google.protobuf.TypeRegistry.newBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class BuildEventSinkProcessorIntegrationTest
{
    private static final String UUID = "cbf29d13-9dd2-41bd-92dc-06ab2d704990";  // UUID.randomUUID().toString()
    private static final OrderedBuildEvent EVENT = OrderedBuildEvent.newBuilder()
            .setSequenceNumber(1)
            .setStreamId(StreamId.newBuilder()
                    .setComponent(StreamId.BuildComponent.CONTROLLER)
                    .setInvocationId(UUID))
            .setEvent(BuildEvent.newBuilder()
                    .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                            .setStarted(BuildEventStreamProtos.BuildStarted.newBuilder().setUuid(UUID))
                            .build()))
                    .build())
            .build();
    private static final String EXPECTED_JSON = "{\"streamId\":{\"component\":\"CONTROLLER\",\"invocationId\":\"cbf29d13-9dd2-41bd-92dc-06ab2d704990\"},\"sequenceNumber\":\"1\",\"event\":{\"bazelEvent\":{\"@type\":\"type.googleapis.com/build_event_stream.BuildEvent\",\"started\":{\"uuid\":\"cbf29d13-9dd2-41bd-92dc-06ab2d704990\"}}}}";

    private final TypeRegistry typeRegistry = newBuilder()
            .add(BuildEventProto.getDescriptor().getMessageTypes())
            .add(BuildEventStreamProtos.getDescriptor().getMessageTypes())
            .build();
    private final JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry).omittingInsignificantWhitespace();

    @Test
    public void shouldEncodeAsJsonAndInsertIntoTable() throws IOException, SQLException
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Writer writer = new OutputStreamWriter(out);
        new BuildEventSinkProcessor(new JsonSink(printer, writer)).accept(EVENT);
        writer.flush();
        final String json = out.toString();

        final Path dbPath = Files.createTempFile("bes", ".db");
        dbPath.toFile().deleteOnExit();
        try (final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath))
        {
            try (final Statement createTable = connection.createStatement())
            {
                createTable.execute("CREATE TABLE bes (uuid TEXT PRIMARY KEY, json TEXT)");
            }
            try (final PreparedStatement insert = connection.prepareStatement("INSERT INTO bes VALUES (?,?)"))
            {
                insert.setString(1, UUID);
                insert.setString(2, json);
                insert.execute();
            }
            try (final PreparedStatement select = connection.prepareStatement("SELECT json FROM bes WHERE uuid = ?"))
            {
                select.setString(1, UUID);
                try (final ResultSet resultSet = select.executeQuery())
                {
                    while (resultSet.next())
                    {
                        assertThat(resultSet.getString("json"), equalTo(EXPECTED_JSON));
                    }
                }
            }
        }
    }
}
