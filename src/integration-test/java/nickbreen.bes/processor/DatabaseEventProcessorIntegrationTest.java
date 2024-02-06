package nickbreen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.BuildEventProto;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.StreamId;
import com.google.protobuf.Any;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import org.junit.Before;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static com.google.protobuf.TypeRegistry.newBuilder;
import static nickbreen.bes.Util.loadBinary;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.blankString;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class DatabaseEventProcessorIntegrationTest
{
    private static final String BUILD_ID = UUID.randomUUID().toString();
    private static final String INVOCATION_ID = UUID.randomUUID().toString();
    public static final long SEQUENCE_NUMBER = 1L;
    private static final OrderedBuildEvent EVENT = OrderedBuildEvent.newBuilder()
            .setSequenceNumber(SEQUENCE_NUMBER)
            .setStreamId(StreamId.newBuilder()
                    .setBuildId(BUILD_ID)
                    .setComponent(StreamId.BuildComponent.CONTROLLER)
                    .setInvocationId(INVOCATION_ID))
            .setEvent(BuildEvent.newBuilder()
                    .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                            .setStarted(BuildEventStreamProtos.BuildStarted.newBuilder().setUuid(INVOCATION_ID))
                            .build()))
                    .build())
            .build();
    private final TypeRegistry typeRegistry = newBuilder()
            .add(BuildEventProto.getDescriptor().getMessageTypes())
            .add(BuildEventStreamProtos.getDescriptor().getMessageTypes())
            .build();
    private final JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry).omittingInsignificantWhitespace();
    private final SQLiteDataSource dataSource = new SQLiteDataSource();

    @Before
    public void setUp()
    {
        dataSource.setUrl("jdbc:sqlite:memory:");
    }

    @Test
    public void shouldEncodeAsJsonAndInsertIntoTable() throws SQLException
    {
        final OrderedBuildEvent event = OrderedBuildEvent.newBuilder()
                .setSequenceNumber(SEQUENCE_NUMBER)
                .setStreamId(StreamId.newBuilder()
                        .setBuildId(BUILD_ID)
                        .setComponent(StreamId.BuildComponent.CONTROLLER)
                        .setInvocationId(INVOCATION_ID))
                .setEvent(BuildEvent.newBuilder()
                        .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                                .setStarted(BuildEventStreamProtos.BuildStarted.newBuilder().setUuid(INVOCATION_ID))
                                .build()))
                        .build())
                .build();
        new DatabaseEventProcessor(dataSource, printer).accept(event);

        try (final Connection connection = dataSource.getConnection())
        {
            try (final PreparedStatement select = connection.prepareStatement("SELECT *, event ->> '$.bazelEvent.started.uuid' as started_uuid FROM event"))
            {
                try (final ResultSet resultSet = select.executeQuery())
                {
                    while (resultSet.next())
                    {
                        assertThat(resultSet.getString("build_id"), equalTo(BUILD_ID));
                        assertThat(resultSet.getInt("component"), equalTo(StreamId.BuildComponent.CONTROLLER.getNumber()));
                        assertThat(resultSet.getString("invocation_id"), equalTo(INVOCATION_ID));
                        assertThat(resultSet.getLong("sequence"), equalTo(SEQUENCE_NUMBER));
                        assertThat(resultSet.getString("started_uuid"), equalTo(INVOCATION_ID));
                    }
                }
            }
        }
    }

    @Test
    public void willNotInsertDuplicates() throws SQLException
    {
        final OrderedBuildEvent event = OrderedBuildEvent.newBuilder()
                .setSequenceNumber(SEQUENCE_NUMBER)
                .setStreamId(StreamId.newBuilder()
                        .setBuildId(BUILD_ID)
                        .setComponent(StreamId.BuildComponent.CONTROLLER)
                        .setInvocationId(INVOCATION_ID))
                .setEvent(BuildEvent.newBuilder()
                        .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                                .setStarted(BuildEventStreamProtos.BuildStarted.newBuilder().setUuid(INVOCATION_ID))
                                .build()))
                        .build())
                .build();
        final DatabaseEventProcessor processor = new DatabaseEventProcessor(dataSource, printer);
        processor.accept(event);
        processor.accept(event);

        try (final Connection connection = dataSource.getConnection())
        {
            try (final PreparedStatement select = connection.prepareStatement("SELECT build_id, COUNT(1) FROM event GROUP BY build_id"))
            {
                try (final ResultSet resultSet = select.executeQuery())
                {
                    while (resultSet.next())
                    {
                        assertThat("a row for UUID", resultSet.getString(1), equalTo(BUILD_ID));
                        assertThat("exactly one row counted", resultSet.getInt(2), equalTo(1));
                    }
                    assertThat("exactly one row", resultSet.getRow(), equalTo(1));
                }
            }
        }
    }

    @Test
    public void shouldIngestEntireJournal() throws IOException, SQLException
    {
        final DatabaseEventProcessor processor = new DatabaseEventProcessor(dataSource, printer);
        final List<OrderedBuildEvent> events = loadBinary(OrderedBuildEvent::parseDelimitedFrom, DatabaseEventProcessorIntegrationTest.class::getResourceAsStream, "/jnl.bin");
        assertThat(events, not(empty()));
        events.forEach(processor::accept);

        try (final Connection connection = dataSource.getConnection())
        {
            try (final PreparedStatement select = connection.prepareStatement("SELECT *, event ->> '$.bazelEvent.started.uuid' AS started_uuid FROM event WHERE build_id = ?"))
            {
                select.setString(1, "c07753e2-5660-40ba-a3d0-8cd932a74fb8");
                try (final ResultSet resultSet = select.executeQuery())
                {
                    while (resultSet.next())
                    {
                        assertThat(String.format("%3d.%s", resultSet.getRow(), "build_id"), resultSet.getString("build_id"), equalToIgnoringCase("c07753e2-5660-40ba-a3d0-8cd932a74fb8"));
                        assertThat(String.format("%3d.%s", resultSet.getRow(), "invocation_id"), resultSet.getString("invocation_id"), either(equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")).or(emptyString()));
                        // TODO Why does this not work the same way? Hunch: it cannot type-safe match null.
                        // assertThat(String.format("%3d.%s", resultSet.getRow(), "started_uuid"), resultSet.getString("started_uuid"), either(nullValue(String.class)).or(equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")));
                        assertThat(String.format("%3d.%s", resultSet.getRow(), "started_uuid"), resultSet.getString("started_uuid"), anyOf(emptyOrNullString(), equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")));
                        assertThat(String.format("%3d.%s", resultSet.getRow(), "started_uuid"), resultSet.getString("started_uuid"), resultSet.wasNull() ? nullValue(String.class) : equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa"));
                    }
                }
            }
        }
    }
}
