package nickbreen.bes;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.ServerBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class Main
{
    private static final Map<String, Function<URI, Consumer<Message>>> SINK_FACTORIES = Map.of(
            "file", uri -> {
                try
                {
                    @SuppressWarnings("resource") // it is used in the lambda, so we cannot try-with-resources
                    final PrintStream out = new PrintStream(new File(uri));
                    return message -> out.printf("*** %s ***:\n%s", message.getClass().getSimpleName(), message);
                }
                catch (FileNotFoundException e)
                {
                    throw new Error(e);
                }
            },
            "redis", uri -> message -> {} // todo no-op for now
    );

    private static BindableService createService(final URI uri)
    {
        final Consumer<Message> sink = SINK_FACTORIES.get(uri.getScheme()).apply(uri);
        final Consumer<Any> bazelBuildEventConsumer = new BazelBuildEventConsumer(sink);
        final BuildEventConsumer buildEventConsumer = new DelegatingBuildEventConsumer(bazelBuildEventConsumer, sink);
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
