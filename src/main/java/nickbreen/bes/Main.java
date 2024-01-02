package nickbreen.bes;

import com.google.protobuf.Message;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Optional;

public class Main
{
    public static void main(final String[] args) throws InterruptedException, IOException
    {
        final BazelBuildEventConsumer bazelBuildEventConsumer = new BazelBuildEventConsumer(Main::log);
        final BuildEventConsumer buildEventConsumer = new BuildEventConsumer(Main::log, bazelBuildEventConsumer::accept);
        final PublishBuildEventService bindableService = new PublishBuildEventService(buildEventConsumer::accept, buildEventConsumer::accept);

        int port = Optional.ofNullable(System.getProperty("port", System.getenv("PORT"))).map(Integer::parseInt).orElse(8888);
        final Server server = ServerBuilder.forPort(port).addService(bindableService).build();

        server.start();
        server.awaitTermination();
    }

    private static void log(final Message arg)
    {
        System.out.printf("*** %s ***:\n%s", arg.getClass().getSimpleName(), arg);
    }
}
