package kiwi.breen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.StreamId;
import com.google.protobuf.Any;
import kiwi.breen.bes.DataSourceFactory;
import kiwi.breen.bes.TestDAO;
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

import static kiwi.breen.bes.TestUtil.loadBinary;
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
    private TestDAO testDao;

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
                new TestParameter(
                        URI.create("jdbc:tc:mysql:///bes?TC_INITSCRIPT=init.mysql.sql"),
                        Optional.empty()),
                new TestParameter(
                        URI.create("jdbc:tc:mariadb:///bes?TC_INITSCRIPT=init.mariadb.sql"),
                        Optional.empty()),
                new TestParameter(
                        URI.create("jdbc:tc:postgresql:///bes?TC_INITSCRIPT=init.postgresql.sql"),
                        Optional.empty()),
                new TestParameter(
                        URI.create("jdbc:sqlite:memory:bes"),
                        Optional.of("/init.sqlite.sql")),
                new TestParameter(
                        URI.create("jdbc:h2:mem:bes"),
                        Optional.of("/init.h2.sql")),
        };
    }

    @Parameter
    public TestParameter testParameter;

    @Before
    public void setUp()
    {
        final DataSource dataSource = DataSourceFactory.buildDataSource(testParameter.jdbcUrl());
        testDao = new TestDAO(dataSource);
        processor = DatabaseProcessor.create(dataSource);
        testParameter.initSql().ifPresent(testDao::init);
    }

    @After
    public void tearDown()
    {
        testDao.clear(); // persists between runs
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

        testDao.allEvents(actualEvent -> {
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
        testDao.allEvents(events::add);
        assertThat(events, hasSize(1));
        testDao.allEvents(actualEvent -> {
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
        testDao.allEvents(actualEvents::add);
        assertThat(actualEvents, hasSize(sourceEvents.size()));
        testDao.allEvents(actualEvent -> {
            // There are two builds in the journal... check for either of them
            assertThat(actualEvent.buildId(), either(equalToIgnoringCase("c07753e2-5660-40ba-a3d0-8cd932a74fb8")).or(equalToIgnoringCase("4d16d0f2-6337-4a12-9129-52097ea30e63")));
            assertThat(actualEvent.invocationId(), either(equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")).or(equalToIgnoringCase("584d2774-aab7-4ce9-8a9a-32ac493b0f6e")).or(blankString()));
            // This looks like it should work, but it won't as the null fails early somewhere
            // assertThat(actualEvent.bazelEventStartedUuid(), either(equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")).or(equalToIgnoringCase("584d2774-aab7-4ce9-8a9a-32ac493b0f6e")).or(emptyOrNullString()));
            assertThat(actualEvent.bazelEventStartedUuid(), anyOf(either(equalToIgnoringCase("e15c3cc2-e9df-4ac0-96c8-e12129bc7caa")).or(equalToIgnoringCase("584d2774-aab7-4ce9-8a9a-32ac493b0f6e")), emptyOrNullString()));
        });
    }
}
