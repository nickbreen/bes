package kiwi.breen.bes;

import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import kiwi.breen.bes.processor.DatabaseProcessorFactory;
import kiwi.breen.bes.processor.JournalProcessor;
import kiwi.breen.bes.processor.PublishEventProcessor;
import kiwi.breen.bes.sink.BinaryWriter;
import kiwi.breen.bes.sink.JsonlWriter;
import kiwi.breen.bes.sink.TextWriter;

import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static kiwi.breen.bes.Util.buildJsonPrinter;
import static kiwi.breen.bes.Util.buildTextPrinter;

class PublishBuildEventProcessor extends PublishBuildEventGrpc.PublishBuildEventImplBase
{
    private final Collection<PublishEventProcessor> processors = new ArrayList<>();

    PublishBuildEventProcessor(final Collection<PublishEventProcessor> processors)
    {
        this.processors.addAll(processors);
    }

    @Override
    public void publishLifecycleEvent(final PublishLifecycleEventRequest request, final StreamObserver<Empty> responseObserver)
    {
        processors.forEach(processor -> processor.accept(request));
    }

    @Override
    public StreamObserver<PublishBuildToolEventStreamRequest> publishBuildToolEventStream(final StreamObserver<PublishBuildToolEventStreamResponse> responseObserver)
    {
        return new StreamObserver<>()
        {
            @Override
            public void onNext(PublishBuildToolEventStreamRequest request)
            {
                processors.forEach(processor -> processor.accept(request));

                final PublishBuildToolEventStreamResponse.Builder builder = PublishBuildToolEventStreamResponse.newBuilder()
                        .setStreamId(request.getOrderedBuildEvent().getStreamId())
                        .setSequenceNumber(request.getOrderedBuildEvent().getSequenceNumber());
                responseObserver.onNext(builder.build());
            }

            @Override
            public void onCompleted()
            {
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable t)
            {
                responseObserver.onError(t);
            }
        };
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Builder
    {
        private Optional<PublishEventProcessor> jdbc;
        private Optional<PublishEventProcessor> binaryJournal;
        private Optional<PublishEventProcessor> jsonJournal;
        private Optional<PublishEventProcessor> textJournal;

        public Builder jdbc(final URI jdbc)
        {
            return jdbc(Optional.ofNullable(jdbc));
        }

        public Builder jdbc(final Optional<URI> jdbc)
        {
            this.jdbc = jdbc
                    .map(DataSourceFactory::buildDataSource)
                    .map(DatabaseProcessorFactory::create);
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
            return new PublishBuildEventProcessor(
                    Stream.of(
                                    jdbc,
                                    binaryJournal,
                                    jsonJournal,
                                    textJournal)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList());
        }
    }
}
