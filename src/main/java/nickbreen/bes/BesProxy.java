package nickbreen.bes;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.devtools.build.v1.PublishBuildEventGrpc;
import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import nickbreen.bes.args.UriConverter;

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

public class BesProxy implements Runnable
{
    private final int port;
    private final URI proxy;

    public BesProxy(final int port, final URI proxy)
    {
        this.port = port;
        this.proxy = proxy;
    }

    @Override
    public void run()
    {
        final ManagedChannelBuilder<?> builder = Grpc.newChannelBuilder(proxy.getAuthority(), InsecureChannelCredentials.create());
        final ManagedChannel channel = builder.build();
        Runtime.getRuntime().addShutdownHook(new Thread(channel::shutdownNow));
        final PublishBuildEventGrpc.PublishBuildEventStub stub = PublishBuildEventGrpc.newStub(channel);
        final BindableService service = new PublishBuildEventProxy(stub);

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

    private static class Args
    {
        @Parameter(names = {"-p", "--port"}, description = "GRPC TCP port to listen on, also system property 'port' or environment variable 'PORT', defaults to 18888")
        int port = Optional.ofNullable(System.getProperty("port", System.getenv("PORT"))).map(Integer::parseInt).orElse(18888);

        @Parameter(converter = UriConverter.class)
        URI proxy;
    }

    public static void main(String[] args)
    {
        final Args parsedArgs = new Args();
        JCommander.newBuilder().addObject(parsedArgs).build().parse(args);

        new BesProxy(parsedArgs.port, parsedArgs.proxy).run();
    }

}
