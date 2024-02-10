package nickbreen.bes;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toUnmodifiableList;

public class BesServer implements Runnable
{
    private final int port;
    private final PublishBuildEventService service;

    public BesServer(final int port, final PublishBuildEventService service)
    {
        this.port = port;
        this.service = service;
    }

    @Override
    public void run()
    {
        final Server server = ServerBuilder.forPort(port).addService(service).build();

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

        try
        {
            server.start().awaitTermination();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }

    public static void main(final String[] args)
    {
        final int port = Optional.ofNullable(System.getProperty("port", System.getenv("PORT"))).map(Integer::parseInt).orElse(8888);
        final PublishBuildEventService service = stream(args)
                .map(URI::create)
                .map(ProcessorFactory::create)
                .collect(collectingAndThen(toUnmodifiableList(), PublishBuildEventService::new));
        new BesServer(port, service).run();
    }
}
