package nickbreen.bes;

import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

import java.util.Collection;
import java.util.stream.Stream;

class PublishBuildEventService extends PublishBuildEventGrpc.PublishBuildEventImplBase
{
    private final Collection<PublishEventProcessor> buildEventProcessors;

    public PublishBuildEventService(final Collection<PublishEventProcessor> buildEventProcessors)
    {
        this.buildEventProcessors = buildEventProcessors;
    }

    @Override
    public void publishLifecycleEvent(final PublishLifecycleEventRequest request, final StreamObserver<Empty> responseObserver)
    {
        buildEventProcessors.forEach(p -> p.accept(request));
        responseObserver.onNext(Empty.newBuilder().getDefaultInstanceForType());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<PublishBuildToolEventStreamRequest> publishBuildToolEventStream(final StreamObserver<PublishBuildToolEventStreamResponse> responseObserver)
    {
        return new StreamObserver<>()
        {
            @Override
            public void onNext(PublishBuildToolEventStreamRequest request)
            {
                buildEventProcessors.forEach(p -> p.accept(request));
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
                responseObserver.onError(new StatusException(Status.fromThrowable(t)));
            }
        };
    }
}
