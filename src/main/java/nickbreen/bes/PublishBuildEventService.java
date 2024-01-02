package nickbreen.bes;

import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

import java.util.function.Consumer;

class PublishBuildEventService extends PublishBuildEventGrpc.PublishBuildEventImplBase
{

    private final Consumer<PublishLifecycleEventRequest> publishLifecycleEventRequestConsumer;
    private final Consumer<PublishBuildToolEventStreamRequest> publishBuildToolEventStreamRequestConsumer;

    public PublishBuildEventService(final Consumer<PublishLifecycleEventRequest> publishLifecycleEventRequestConsumer, final Consumer<PublishBuildToolEventStreamRequest> publishBuildToolEventStreamRequestConsumer)
    {
        this.publishLifecycleEventRequestConsumer = publishLifecycleEventRequestConsumer;
        this.publishBuildToolEventStreamRequestConsumer = publishBuildToolEventStreamRequestConsumer;
    }

    @Override
    public void publishLifecycleEvent(final PublishLifecycleEventRequest request, final StreamObserver<Empty> responseObserver)
    {
        publishLifecycleEventRequestConsumer.accept(request);
        responseObserver.onNext(Empty.newBuilder().getDefaultInstanceForType());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<PublishBuildToolEventStreamRequest> publishBuildToolEventStream(final StreamObserver<PublishBuildToolEventStreamResponse> responseObserver)
    {
        return new StreamObserver<PublishBuildToolEventStreamRequest>()
        {
            @Override
            public void onNext(PublishBuildToolEventStreamRequest request)
            {
                publishBuildToolEventStreamRequestConsumer.accept(request);
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
