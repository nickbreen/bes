package kiwi.breen.bes;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.URIConverter;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import kiwi.breen.bes.processor.BuildEventProcessor;

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
                    // nothing
                }

                @Override
                public void onError(final Throwable t)
                {
                    // nothing
                }

                @Override
                public void onCompleted()
                {
                    // nothing
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
                channel.shutdown().awaitTermination(15, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static BesClient create(final URI uri)
    {
        return new BesClient(Grpc.newChannelBuilder(uri.getAuthority(), InsecureChannelCredentials.create()));
    }

    private static class Args
    {
        @Parameter(names = {"-t", "--type"}, description = "Journal type, defaults to binary")
        JournalType journalType = JournalType.binary;

        @Parameter(converter = URIConverter.class, description = "GRPC destination")
        URI destination;
    }

    public static void main(final String[] args)
    {
        final Args parsedArgs = new Args();
        JCommander.newBuilder().addObject(parsedArgs).args(args).build();

        final JsonFormat.Parser parser = JsonFormat.parser().usingTypeRegistry(BuildEventProcessor.buildTypeRegistry());
        try
        {
            BesClient.create(parsedArgs.destination)
                    .accept(switch (parsedArgs.journalType)
                    {
                        case binary -> TestUtil.parseBinary(
                                OrderedBuildEvent::parseDelimitedFrom,
                                System.in);
                        case json -> TestUtil.parseDelimitedJson(
                                OrderedBuildEvent.newBuilder(),
                                parser,
                                System.in).stream();
                        case text -> throw new UnsupportedOperationException("text format not supported");
                    });
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }

    private enum JournalType
    {
        binary, json, text
    }
}
