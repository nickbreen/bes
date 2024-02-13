package nickbreen.bes;


import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
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

    public static BesClient create(final String host, final int port)
    {
        return new BesClient(Grpc.newChannelBuilderForAddress(host, port, InsecureChannelCredentials.create()));
    }

    private static class Args
    {
        @Parameter(names = {"-p", "--port"}, description = "Destination TCP port, defaults to 8888")
        int port = 8888;

        @Parameter(names = {"-h", "--host"}, description = "Destination host, defaults to localhost")
        String host = "localhost";

        @Parameter(names = {"-t", "--type"}, description = "Journal type, defaults to binary")
        JournalType journalType = JournalType.binary;

        @Parameter(converter = InputStreamConverter.class, description = "Journal file, defaults to stdin")
        InputStream journal = System.in;

        static class InputStreamConverter implements IStringConverter<InputStream>
        {
            @Override
            public InputStream convert(final String value)
            {
                try
                {
                    return new FileInputStream(value);
                }
                catch (FileNotFoundException e)
                {
                    throw new IOError(e);
                }
            }
        }
    }

    public static void main(final String[] args)
    {
        final Args parsedArgs = new Args();
        JCommander.newBuilder().addObject(parsedArgs).args(args).build();

        try
        {
            BesClient.create(parsedArgs.host, parsedArgs.port)
                    .accept(switch (parsedArgs.journalType)
                    {
                        case binary -> Util.parseBinary(OrderedBuildEvent::parseDelimitedFrom, parsedArgs.journal);
                        case json -> Util.parseDelimitedJson(OrderedBuildEvent.newBuilder(), parsedArgs.journal);
                        case text -> throw new UnsupportedOperationException("text format not supported");
                    });
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }
}
