package kiwi.breen.bes;

import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import kiwi.breen.bes.processor.PublishEventProcessor;

import java.util.ArrayList;
import java.util.Collection;

public class PublishBuildEventProcessor extends PublishBuildEventGrpc.PublishBuildEventImplBase
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
}
