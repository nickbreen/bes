package nickbreen.bes;

import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

public class PublishBuildEventProxy extends PublishBuildEventGrpc.PublishBuildEventImplBase
{
    private final PublishBuildEventGrpc.PublishBuildEventStub stub;

    public PublishBuildEventProxy(final PublishBuildEventGrpc.PublishBuildEventStub stub)
    {
        this.stub = stub;
    }

    @Override
    public void publishLifecycleEvent(final PublishLifecycleEventRequest request, final StreamObserver<Empty> responseObserver)
    {
        stub.publishLifecycleEvent(request, responseObserver);
    }

    @Override
    public StreamObserver<PublishBuildToolEventStreamRequest> publishBuildToolEventStream(final StreamObserver<PublishBuildToolEventStreamResponse> responseObserver)
    {
        return stub.publishBuildToolEventStream(responseObserver);
    }
}
