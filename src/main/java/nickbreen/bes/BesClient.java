package nickbreen.bes;


import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class BesClient implements Consumer<Stream<OrderedBuildEvent>>
{
    private final ManagedChannelBuilder<?> builder;

    protected BesClient(final ManagedChannelBuilder<?> builder)
    {
        this.builder = builder;
    }

    @Override
    public void accept(final Stream<OrderedBuildEvent> orderedBuildEvents)
    {
        final ManagedChannel channel = builder.build();
        final PublishBuildEventGrpc.PublishBuildEventStub stub = PublishBuildEventGrpc.newStub(channel);
        try
        {
            final StreamObserver<PublishBuildToolEventStreamRequest> requestObserver = stub.publishBuildToolEventStream(new StreamObserver<>()
            {
                @Override
                public void onNext(final PublishBuildToolEventStreamResponse value)
                {
                    // TODO if anything
                }

                @Override
                public void onError(final Throwable t)
                {
                    channel.shutdownNow();
                }

                @Override
                public void onCompleted()
                {
                    channel.shutdown();
                }
            });
            final PublishBuildToolEventStreamRequest.Builder builder = PublishBuildToolEventStreamRequest.newBuilder();

            orderedBuildEvents
                    .map(builder::setOrderedBuildEvent)
                    .map(PublishBuildToolEventStreamRequest.Builder::build)
                    .forEach(requestObserver::onNext);
            requestObserver.onCompleted();
        }
        finally
        {
            try
            {
                channel.awaitTermination(5, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static BesClient create(final String authority)
    {
        return new BesClient(Grpc.newChannelBuilder(authority, InsecureChannelCredentials.create()));
    }

    public static void main(String[] args)
    {
        assert args.length == 1;
        final URI uri = URI.create(args[0]);
        assert uri.isAbsolute();
        assert null != uri.getAuthority();

        try
        {
            BesClient.create(uri.getAuthority()).accept(Util.parseBinary(OrderedBuildEvent::parseDelimitedFrom, System.in));
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }

}
