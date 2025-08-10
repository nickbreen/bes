package kiwi.breen.bes;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.util.function.Consumer;

class OrderedBuildEventService extends PublishBuildEventGrpc.PublishBuildEventImplBase
{
    private final Consumer<OrderedBuildEvent> sink;

    public OrderedBuildEventService(final Consumer<OrderedBuildEvent> sink)
    {
        this.sink = sink;
    }

    @Override
    public void publishLifecycleEvent(final PublishLifecycleEventRequest request, final StreamObserver<Empty> responseObserver)
    {
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
        sink.accept(request.getBuildEvent());
    }

    @Override
    public StreamObserver<PublishBuildToolEventStreamRequest> publishBuildToolEventStream(final StreamObserver<PublishBuildToolEventStreamResponse> responseObserver)
    {
        final PublishBuildToolEventStreamResponse.Builder builder = PublishBuildToolEventStreamResponse.newBuilder();
        return new StreamObserver<>()
        {
            @Override
            public void onNext(final PublishBuildToolEventStreamRequest request)
            {
                sink.accept(request.getOrderedBuildEvent());
                builder.clear()
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
            public void onError(final Throwable t)
            {
                responseObserver.onError(t);
            }
        };
    }
}
