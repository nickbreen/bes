package nickbreen.bes;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.util.JsonFormat;
import nickbreen.bes.data.TestDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ContainerAcceptanceTest
{
    private static final String JRE_IMAGE = "eclipse-temurin:17-jre";
    private static final MountableFile BEC_JAR = Optional.of(System.getProperty("bec.uber.jar")).map(Path::of).filter(Files::exists).map(MountableFile::forHostPath).orElseThrow();
    private static final MountableFile BES_JAR = Optional.of(System.getProperty("bes.uber.jar")).map(Path::of).filter(Files::exists).map(MountableFile::forHostPath).orElseThrow();
    private static final MountableFile BINARY_JOURNAL = MountableFile.forClasspathResource("/jnl.bin");

    @SuppressWarnings("resource")
    final PostgreSQLContainer<?> db = new PostgreSQLContainer<>(DockerImageName.parse(PostgreSQLContainer.IMAGE).withTag(PostgreSQLContainer.DEFAULT_TAG))
            .withDatabaseName("bes")
            .withNetworkAliases("db")
            .withNetwork(Network.SHARED);

    @SuppressWarnings("resource")
    public final GenericContainer<?> bes = new GenericContainer<>(DockerImageName.parse(JRE_IMAGE))
            .dependsOn(db)
            .withNetwork(Network.SHARED)
            .withNetworkAliases("bes")
            .withCopyFileToContainer(BES_JAR, "/bes.jar")
            .withCommand("java", "-jar", "/bes.jar", "--port", "8888", "--database", "jdbc:postgresql://db/bes?user=test&password=test", "--json-journal", "/tmp/jnl.jsonl");

    @SuppressWarnings("resource")
    final GenericContainer<?> bec = new GenericContainer<>(DockerImageName.parse(JRE_IMAGE))
            .dependsOn(bes)
            .withNetwork(Network.SHARED)
            .withCopyFileToContainer(BEC_JAR, "/bec.jar")
            .withCopyFileToContainer(BINARY_JOURNAL, "/jnl.bin")
            .withCommand("sh", "-c", "java -jar bec.jar grpc://bes:8888 < jnl.bin")
            .withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy());

    private TestDAO dao;

    @Before
    public void setUp()
    {
        db.start();

        dao = new TestDAO(DataSourceFactory.buildDataSource(URI.create(db.getJdbcUrl()), db.getUsername(), db.getPassword()));
        dao.init("/init.postgresql.sql");
    }

    @After
    public void tearDown()
    {
        db.stop();
    }

    @Test
    public void shouldSimulateBuildEventsToServerAndDB()
    {
        bes.start();

        bec.start();

        final List<TestDAO.Event> events = new ArrayList<>();
        dao.allEventsByBuild(events::add, "c07753e2-5660-40ba-a3d0-8cd932a74fb8");

        assertThat(events, hasSize(48));

        bes.stop();
    }

    @Test
    public void shouldSimulateBuildEventsToServerAndJsonJournal() throws IOException
    {
        bes.start();

        bec.start();

        final OrderedBuildEvent.Builder builder = OrderedBuildEvent.newBuilder();
        final JsonFormat.Parser parser = Util.buildJsonParser();
        final List<OrderedBuildEvent> actualEvents = bes.copyFileFromContainer("/tmp/jnl.jsonl", inputStream -> Util.parseDelimitedJson(builder, parser, inputStream));
        final List<OrderedBuildEvent> expectedEvents = TestUtil.loadJsonl(builder, parser, ContainerAcceptanceTest.class::getResourceAsStream, "/jnl.jsonl");

        assertThat(actualEvents, equalTo(expectedEvents));

        bes.stop();
    }
}
