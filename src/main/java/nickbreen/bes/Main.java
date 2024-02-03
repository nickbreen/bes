package nickbreen.bes;

import io.grpc.ServerBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public class Main
{
    public static void main(final String[] args) throws InterruptedException, IOException
    {
        final int port = Optional.ofNullable(System.getProperty("port", System.getenv("PORT"))).map(Integer::parseInt).orElse(8888);
        ServerBuilder.forPort(port)
                .addService(stream(args)
                        .map(URI::create)
                        .map(PublishEventProcessor.Factory::create)
                        .collect(collectingAndThen(toList(), PublishBuildEventService::new)))
                .build()
                .start()
                .awaitTermination();
    }
}
