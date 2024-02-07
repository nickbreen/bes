package nickbreen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.BuildEventProto;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.StreamId;
import com.google.protobuf.Any;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import nickbreen.bes.DataSourceFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.protobuf.TypeRegistry.newBuilder;
import static nickbreen.bes.DataSourceFactory.loadDbProperties;
import static nickbreen.bes.Util.loadBinary;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.blankString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@RunWith(Parameterized.class)
public class DatabaseEventProcessorIntegrationTest
{
    private static final String BUILD_ID = UUID.randomUUID().toString();
    private static final String INVOCATION_ID = UUID.randomUUID().toString();
    public static final long SEQUENCE_NUMBER = 1L;
    private DataSource dataSource;
    private DatabaseEventProcessor processor;

    @Parameters(name = "{0}")
    public static Object[][] parameters()
    {
        return new Object[][]{
                new Object[]{URI.create("jdbc:tc:mysql:///bes?TC_INITSCRIPT=init.sql"), null},
                new Object[]{URI.create("jdbc:tc:mariadb:///bes?TC_INITSCRIPT=init.sql"), null},
                new Object[]{URI.create("jdbc:tc:postgresql:///bes?TC_INITSCRIPT=init.postgresql.sql"), null},
                new Object[]{URI.create("jdbc:sqlite:memory:"), "/init.sqlite.sql"},
                // URI.create("jdbc:h2:mem:"),
        };
    }

    @Parameter
    public URI jdbcUrl;
    @Parameter(1)
    public String initSql;

    @Before
    public void setUp() throws SQLException
    {
        final TypeRegistry typeRegistry = newBuilder()
                .add(BuildEventProto.getDescriptor().getMessageTypes())
                .add(BuildEventStreamProtos.getDescriptor().getMessageTypes())
                .build();
        final JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry).omittingInsignificantWhitespace();
        dataSource = DataSourceFactory.buildDataSource(jdbcUrl);
        processor = new DatabaseEventProcessor(dataSource, printer);
        Optional.ofNullable(initSql).ifPresent(uri -> DatabaseEventProcessor.init(dataSource, uri));
        dataSource.getConnection().createStatement().execute("DELETE FROM event"); // persists between runs
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
        processor.accept(event);

        try (final Connection connection = dataSource.getConnection())
        {
            final String sql = switch (connection.getMetaData().getDatabaseProductName().toLowerCase())
            {
                case "mariadb" -> "SELECT *, JSON_VALUE(event, '$.bazelEvent.started.uuid') AS started_uuid FROM event";
                case "postgresql" -> "SELECT *, event #>> '{bazelEvent,started,uuid}' AS started_uuid FROM event";
                default -> "SELECT *, event ->> '$.bazelEvent.started.uuid' AS started_uuid FROM event";
            };
            try (final PreparedStatement select = connection.prepareStatement(sql))
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
        processor.accept(event);
        processor.accept(event);

        try (final Connection connection = dataSource.getConnection())
        {
            try (final PreparedStatement select = connection.prepareStatement("SELECT build_id, COUNT(1) FROM event GROUP BY build_id"))
            {
                try (final ResultSet resultSet = select.executeQuery())
                {
                    int i = 0;
                    while (resultSet.next())
                    {
                        assertThat("a row for UUID", resultSet.getString(1), equalTo(BUILD_ID));
                        assertThat("exactly one row counted", resultSet.getInt(2), equalTo(1));
                        i++;
                    }
                    assertThat(i, equalTo(1));
                }
            }
        }
    }

    @Test
    public void shouldIngestEntireJournal() throws IOException, SQLException
    {
        final List<OrderedBuildEvent> events = loadBinary(OrderedBuildEvent::parseDelimitedFrom, DatabaseEventProcessorIntegrationTest.class::getResourceAsStream, "/jnl.bin");
        assertThat(events, not(empty()));
        events.forEach(processor::accept);

        try (final Connection connection = dataSource.getConnection())
        {
            final String sql = switch (connection.getMetaData().getDatabaseProductName().toLowerCase())
            {
                case "mariadb" -> "SELECT *, JSON_VALUE(event, '$.bazelEvent.started.uuid') AS started_uuid FROM event WHERE build_id = ?";
                case "postgresql" -> "SELECT *, event #>> '{bazelEvent,started,uuid}' AS started_uuid FROM event WHERE build_id = ?";
                default -> "SELECT *, event ->> '$.bazelEvent.started.uuid' AS started_uuid FROM event WHERE build_id = ?";
            };
            try (final PreparedStatement select = connection.prepareStatement(sql))
            {
                select.setString(1, "c07753e2-5660-40ba-a3d0-8cd932a74fb8");
                try (final ResultSet resultSet = select.executeQuery())
                {
                    while (resultSet.next())
                    {
                        assertThat(String.format("%3d.%s", resultSet.getRow(), "build_id"), resultSet.getString("build_id"), equalToIgnoringCase("c07753e2-5660-40ba-a3d0-8cd932a74fb8"));
                        assertThat(String.format("%3d.%s", resultSet.getRow(), "invocation_id"), resultSet.getString("invocation_id"), either(equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")).or(blankString()));
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
