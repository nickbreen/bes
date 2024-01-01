package nickbreen.bes;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Optional;

public class Main
{
    public static void main(final String[] args) throws InterruptedException, IOException
    {
        final BuildEventConsumer consumer = new BuildEventConsumer(System.out);
        final PublishBuildEventService bindableService = new PublishBuildEventService(consumer::accept, consumer::accept);

        int port = Optional.ofNullable(System.getProperty("port", System.getenv("PORT"))).map(Integer::parseInt).orElse(8888);
        final Server server = ServerBuilder.forPort(port).addService(bindableService).build();

        server.start();
        server.awaitTermination();
    }

}
