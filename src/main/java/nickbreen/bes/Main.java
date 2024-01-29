package nickbreen.bes;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

public class Main
{
    public static void main(final String[] args) throws InterruptedException, IOException
    {
        final Consumer<Any> bazelBuildEventConsumer = new BazelBuildEventConsumer(Main::log);
        final BuildEventConsumer buildEventConsumer = new DelegatingBuildEventConsumer(bazelBuildEventConsumer, Main::log);
        final PublishBuildEventService bindableService = new PublishBuildEventService(buildEventConsumer::accept, buildEventConsumer::accept);

        final BuildEventConsumer journal = new Journal(System.out);
        final PublishBuildEventService journalService = new PublishBuildEventService(journal::accept, journal::accept);

        int port = Optional.ofNullable(System.getProperty("port", System.getenv("PORT"))).map(Integer::parseInt).orElse(8888);
        final Server server = ServerBuilder.forPort(port).addService(journalService).addService(bindableService).build();

        server.start();
        server.awaitTermination();
    }

    private static void log(final Message arg)
    {
        System.err.printf("*** %s ***:\n%s", arg.getClass().getSimpleName(), arg);
    }
}
