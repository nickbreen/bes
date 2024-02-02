package nickbreen.bes;

import com.google.devtools.build.v1.BuildEvent;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.ServerBuilder;

import java.io.*;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class Main
{
    private static final Map<String, Function<URI, Consumer<BuildEvent>>> SINK_FACTORIES = Map.of(
            "file", uri -> {
                try
                {
                    @SuppressWarnings("resource") // it is used in the lambda, so we cannot try-with-resources
                    final OutputStream outputStream = new FileOutputStream(new File(uri));
                    return message -> {
                        try
                        {
                            message.writeDelimitedTo(outputStream);
                        }
                        catch (final IOException e)
                        {
                            throw new IOError(e);
                        }
                    };
                }
                catch (FileNotFoundException e)
                {
                    throw new IOError(e);
                }
            },
            "redis", uri -> message -> {} // todo no-op for now
    );

    private static BindableService createService(final URI uri)
    {
        final BaseBuildEventConsumer bazelBuildEventConsumer = new BazelBuildEventConsumer(System.out::print);
        final Consumer<BuildEvent> sink = SINK_FACTORIES.get(uri.getScheme()).apply(uri);
        final BaseBuildEventConsumer buildEventConsumer = DelegatingBuildEventConsumer.create(sink, bazelBuildEventConsumer::accept);
        return new PublishBuildEventService(buildEventConsumer::accept, buildEventConsumer::accept);
    }

    public static void main(final String[] args) throws InterruptedException, IOException
    {
        final int port = Optional.ofNullable(System.getProperty("port", System.getenv("PORT"))).map(Integer::parseInt).orElse(8888);
        final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
        Arrays.stream(args).map(URI::create).map(Main::createService).forEach(serverBuilder::addService);
        serverBuilder.build().start().awaitTermination();
    }

}
