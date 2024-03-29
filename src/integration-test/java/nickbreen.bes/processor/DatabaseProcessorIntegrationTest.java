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
import nickbreen.bes.data.EventDAO;
import nickbreen.bes.data.TestDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.google.protobuf.TypeRegistry.newBuilder;
import static nickbreen.bes.TestUtil.loadBinary;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.blankString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@RunWith(Parameterized.class)
public class DatabaseProcessorIntegrationTest
{
    private static final String BUILD_ID = UUID.randomUUID().toString();
    private static final String INVOCATION_ID = UUID.randomUUID().toString();
    public static final long SEQUENCE_NUMBER = 1L;
    private DatabaseProcessor processor;
    private TestDAO dao;

    public record TestParameter(URI jdbcUrl, Optional<String> initSql)
    {
        @Override
        public String toString()
        {
            return jdbcUrl.toString().split("\\?")[0];
        }
    }

    @Parameters(name = "{0}")
    public static TestParameter[] parameters()
    {
        return new TestParameter[]{
                new TestParameter(URI.create("jdbc:tc:mysql:///bes?TC_INITSCRIPT=init.sql"), Optional.empty()),
                new TestParameter(URI.create("jdbc:tc:mariadb:///bes?TC_INITSCRIPT=init.sql"), Optional.empty()),
                new TestParameter(URI.create("jdbc:tc:postgresql:///bes?TC_INITSCRIPT=init.postgresql.sql"), Optional.empty()),
                new TestParameter(URI.create("jdbc:sqlite:memory:"), Optional.of("/init.sqlite.sql")),
                // URI.create("jdbc:h2:mem:"),
        };
    }

    @Parameter
    public TestParameter testParameter;

    @Before
    public void setUp()
    {
        final TypeRegistry typeRegistry = newBuilder()
                .add(BuildEventProto.getDescriptor().getMessageTypes())
                .add(BuildEventStreamProtos.getDescriptor().getMessageTypes())
                .build();
        final JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry).omittingInsignificantWhitespace();
        DataSource dataSource = DataSourceFactory.buildDataSource(testParameter.jdbcUrl());
        dao = new TestDAO(dataSource);
        processor = new DatabaseProcessor(new EventDAO(dataSource), printer);
        testParameter.initSql().ifPresent(dao::init);
    }

    @After
    public void tearDown()
    {
        dao.clear(); // persists between runs
        // TODO run test in transaction?
    }

    @Test
    public void shouldEncodeAsJsonAndInsertIntoTable()
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

        dao.allEvents(actualEvent -> {
            assertThat(actualEvent.sequence(), equalTo(SEQUENCE_NUMBER));
            assertThat(actualEvent.buildId(), equalTo(BUILD_ID));
            assertThat(actualEvent.component(), equalTo(StreamId.BuildComponent.CONTROLLER.getNumber()));
            assertThat(actualEvent.invocationId(), equalTo(INVOCATION_ID));
            assertThat(actualEvent.bazelEventStartedUuid(), equalTo(INVOCATION_ID));
        });
    }

    @Test
    public void willNotInsertDuplicates()
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
                                .build())))
                .build();
        processor.accept(event);
        processor.accept(event);

        final List<TestDAO.Event> events = new ArrayList<>();
        dao.allEvents(events::add);
        assertThat(events, hasSize(1));
        dao.allEvents(actualEvent -> {
            assertThat(actualEvent.sequence(), equalTo(SEQUENCE_NUMBER));
            assertThat(actualEvent.buildId(), equalTo(BUILD_ID));
            assertThat(actualEvent.component(), equalTo(StreamId.BuildComponent.CONTROLLER.getNumber()));
            assertThat(actualEvent.invocationId(), equalTo(INVOCATION_ID));
            assertThat(actualEvent.bazelEventStartedUuid(), equalTo(INVOCATION_ID));
        });
    }

    @Test
    public void shouldIngestEntireJournal() throws IOException
    {
        final List<OrderedBuildEvent> sourceEvents = loadBinary(OrderedBuildEvent::parseDelimitedFrom, DatabaseProcessorIntegrationTest.class::getResourceAsStream, "/jnl.bin");
        assertThat(sourceEvents, not(empty()));
        sourceEvents.forEach(processor::accept);

        final List<TestDAO.Event> actualEvents = new ArrayList<>();
        dao.allEvents(actualEvents::add);
        assertThat(actualEvents, hasSize(sourceEvents.size()));
        dao.allEvents(actualEvent -> {
            // There are two builds in the journal... check for either of them
            assertThat(actualEvent.buildId(), either(equalToIgnoringCase("c07753e2-5660-40ba-a3d0-8cd932a74fb8")).or(equalToIgnoringCase("4d16d0f2-6337-4a12-9129-52097ea30e63")));
            assertThat(actualEvent.invocationId(), either(equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")).or(equalToIgnoringCase("584d2774-aab7-4ce9-8a9a-32ac493b0f6e")).or(blankString()));
            // This looks like it should work, but it won't as the null fails early somewhere
            // assertThat(actualEvent.bazelEventStartedUuid(), either(equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")).or(equalToIgnoringCase("584d2774-aab7-4ce9-8a9a-32ac493b0f6e")).or(emptyOrNullString()));
            assertThat(actualEvent.bazelEventStartedUuid(), anyOf(either(equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")).or(equalToIgnoringCase("584d2774-aab7-4ce9-8a9a-32ac493b0f6e")), emptyOrNullString()));
        });
    }
}
