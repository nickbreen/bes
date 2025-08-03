package kiwi.breen.bes;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.util.JsonFormat;
import kiwi.breen.bes.processor.BuildEventProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static kiwi.breen.bes.TestUtil.loadBinary;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ContainerAcceptanceTest
{
    private static final String JRE_IMAGE = "eclipse-temurin:21-jre-alpine";
    private static final MountableFile BES_JAR = Optional.of("bes.uber.jar")
            .map(System::getProperty)
            .map(Path::of)
            .filter(Files::exists)
            .map(MountableFile::forHostPath)
            .orElseThrow();

    @SuppressWarnings("resource")
    @Rule
    public final PostgreSQLContainer<?> db = new PostgreSQLContainer<>(
            DockerImageName.parse(PostgreSQLContainer.IMAGE)
                    .withTag(PostgreSQLContainer.DEFAULT_TAG))
            .withDatabaseName("bes")
            .withNetworkAliases("db")
            .withNetwork(Network.SHARED);

    @SuppressWarnings("resource")
    @Rule
    public final GenericContainer<?> bes = new GenericContainer<>(DockerImageName.parse(JRE_IMAGE))
            .dependsOn(db)
            .withNetwork(Network.SHARED)
            .withNetworkAliases("bes")
            .withCopyFileToContainer(BES_JAR, "/bes.jar")
            .withExposedPorts(8888)
            .withCommand(
                    "java",
                    "-jar",
                    "/bes.jar",
                    "--port",
                    "8888",
                    "--database",
                    "jdbc:postgresql://db/bes?user=test&password=test",
                    "--json-journal",
                    "/tmp/jnl.jsonl");

    private TestDAO dao;
    private BesClient besClient;

    @Before
    public void setUp() throws URISyntaxException
    {
        final URI uri = URI.create(db.getJdbcUrl());
        final String username = db.getUsername();
        dao = new TestDAO(DataSourceFactory.buildDataSource(uri, username, db.getPassword()));
        dao.init("/init.postgresql.sql");

        final URI grpc = new URI(
                "grpc",
                null,
                bes.getHost(),
                bes.getFirstMappedPort(),
                null,
                null,
                null);
        besClient = BesClient.create(grpc);
    }

    @After
    public void tearDown()
    {
        db.stop();
    }

    @Test
    public void shouldSimulateBuildEventsToServerAndDB() throws IOException, URISyntaxException
    {
        final List<OrderedBuildEvent> expectedEvents = loadBinary(
                OrderedBuildEvent::parseDelimitedFrom,
                AcceptanceTest.class::getResourceAsStream,
                "/jnl.bin");
        besClient.accept(expectedEvents.stream());

        final List<TestDAO.Event> actualEvents = new ArrayList<>();
        dao.allEventsByBuild(actualEvents::add, "c07753e2-5660-40ba-a3d0-8cd932a74fb8");

        assertThat(actualEvents, hasSize(48));
    }

    @Test
    public void shouldSimulateBuildEventsToServerAndJsonJournal() throws IOException, URISyntaxException
    {
        final OrderedBuildEvent.Builder builder = OrderedBuildEvent.newBuilder();
        final JsonFormat.Parser parser = JsonFormat.parser()
                .usingTypeRegistry(BuildEventProcessor.buildTypeRegistry());
        final List<OrderedBuildEvent> expectedEvents = TestUtil.loadJsonl(
                builder,
                parser,
                ContainerAcceptanceTest.class::getResourceAsStream,
                "/jnl.jsonl");
        besClient.accept(expectedEvents.stream());

        final List<OrderedBuildEvent> actualEvents = bes.copyFileFromContainer(
                "/tmp/jnl.jsonl",
                inputStream -> TestUtil.parseDelimitedJson(builder, parser, inputStream));

        assertThat(actualEvents, equalTo(expectedEvents));
    }
}
