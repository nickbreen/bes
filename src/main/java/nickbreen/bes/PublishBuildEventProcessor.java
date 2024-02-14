package nickbreen.bes;

import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import nickbreen.bes.data.EventDAO;
import nickbreen.bes.processor.DatabaseProcessor;
import nickbreen.bes.processor.JournalProcessor;
import nickbreen.bes.processor.PublishEventProcessor;
import nickbreen.bes.sink.BinaryWriter;
import nickbreen.bes.sink.JsonlWriter;
import nickbreen.bes.sink.TextWriter;

import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static nickbreen.bes.Util.buildJsonPrinter;
import static nickbreen.bes.Util.buildTextPrinter;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class PublishBuildEventProcessor extends PublishBuildEventGrpc.PublishBuildEventImplBase
{
    private final Optional<PublishBuildEventGrpc.PublishBuildEventStub> stub;
    private final Collection<PublishEventProcessor> processors = new ArrayList<>();

    PublishBuildEventProcessor(final Optional<PublishBuildEventGrpc.PublishBuildEventStub> stub, final Collection<PublishEventProcessor> processors)
    {
        this.stub = stub;
        this.processors.addAll(processors);
    }

    @Override
    public void publishLifecycleEvent(final PublishLifecycleEventRequest request, final StreamObserver<Empty> responseObserver)
    {
        processors.forEach(processor -> processor.accept(request));
        stub.ifPresentOrElse(
                s -> s.publishLifecycleEvent(request, responseObserver),
                () -> {
                    responseObserver.onNext(Empty.getDefaultInstance());
                    responseObserver.onCompleted();
                });
    }

    @Override
    public StreamObserver<PublishBuildToolEventStreamRequest> publishBuildToolEventStream(final StreamObserver<PublishBuildToolEventStreamResponse> responseObserver)
    {
        final Optional<StreamObserver<PublishBuildToolEventStreamRequest>> maybeRequestObserver = stub.map(p -> p.publishBuildToolEventStream(responseObserver));

        return new StreamObserver<>()
        {
            @Override
            public void onNext(PublishBuildToolEventStreamRequest request)
            {
                processors.forEach(processor -> processor.accept(request));

                maybeRequestObserver.ifPresentOrElse(
                        requestObserver -> requestObserver.onNext(request),
                        () -> {
                            final PublishBuildToolEventStreamResponse.Builder builder = PublishBuildToolEventStreamResponse.newBuilder()
                                    .setStreamId(request.getOrderedBuildEvent().getStreamId())
                                    .setSequenceNumber(request.getOrderedBuildEvent().getSequenceNumber());
                            responseObserver.onNext(builder.build());
                        });
            }

            @Override
            public void onCompleted()
            {
                maybeRequestObserver.ifPresentOrElse(StreamObserver::onCompleted, responseObserver::onCompleted);
            }

            @Override
            public void onError(Throwable t)
            {
                maybeRequestObserver.ifPresentOrElse(requestObserver -> requestObserver.onError(t), () -> responseObserver.onError(t));
            }
        };
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Builder
    {
        private Optional<PublishBuildEventGrpc.PublishBuildEventStub> stub;
        private Optional<PublishEventProcessor> jdbc;
        private Optional<PublishEventProcessor> binaryJournal;
        private Optional<PublishEventProcessor> jsonJournal;
        private Optional<PublishEventProcessor> textJournal;

        public Builder proxy(final URI proxy)
        {
            return proxy(Optional.ofNullable(proxy));
        }

        public Builder proxy(final Optional<URI> proxy)
        {
            this.stub = proxy
                    .map(URI::getAuthority)
                    .map(ManagedChannelBuilder::forTarget)
                    .map(ManagedChannelBuilder::build)
                    .map(PublishBuildEventGrpc::newStub);
            return this;
        }

        public Builder jdbc(final URI jdbc)
        {
            return jdbc(Optional.ofNullable(jdbc));
        }

        public Builder jdbc(final Optional<URI> jdbc)
        {
            this.jdbc = jdbc
                    .map(DataSourceFactory::buildDataSource)
                    .map(EventDAO::new)
                    .map(dao -> new DatabaseProcessor(dao, buildJsonPrinter()));
            return this;
        }

        public Builder binaryJournal(final Path path)
        {
            return binaryJournal(Optional.ofNullable(path));
        }

        private Builder binaryJournal(final Optional<Path> path)
        {
            this.binaryJournal = path
                    .map(Path::toUri)
                    .map(SinkFactory::createSink)
                    .map(BinaryWriter::new)
                    .map(JournalProcessor::new);
            return this;
        }

        public Builder jsonJournal(final Path path)
        {
            return jsonJournal(Optional.ofNullable(path));
        }

        public Builder jsonJournal(final Optional<Path> path)
        {
            this.jsonJournal = path
                    .map(Path::toUri)
                    .map(SinkFactory::createSink)
                    .map(out -> new PrintWriter(out, true))
                    .map(writer -> new JsonlWriter(buildJsonPrinter(), writer))
                    .map(JournalProcessor::new);
            return this;
        }

        public Builder textJournal(final Path path)
        {
            return textJournal(Optional.ofNullable(path));
        }

        public Builder textJournal(final Optional<Path> path)
        {
            this.textJournal = path
                    .map(Path::toUri)
                    .map(SinkFactory::createSink)
                    .map(out -> new PrintWriter(out, true))
                    .map(writer -> new TextWriter(buildTextPrinter(), writer))
                    .map(JournalProcessor::new);
            return this;
        }

        public PublishBuildEventProcessor build()
        {
            return new PublishBuildEventProcessor(stub, Stream.of(jdbc, binaryJournal, jsonJournal, textJournal).filter(Optional::isPresent).map(Optional::get).toList());
        }
    }
}
