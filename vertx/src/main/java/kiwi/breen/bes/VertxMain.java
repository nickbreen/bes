package kiwi.breen.bes;

import com.google.devtools.build.v1.PublishBuildEventGrpc;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishBuildToolEventStreamResponse;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import io.vertx.core.Vertx;
import io.vertx.grpcio.server.GrpcIoServer;
import io.vertx.grpcio.server.GrpcIoServiceBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VertxMain
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VertxMain.class);

    public static void main(String[] args)
    {
        final Vertx vertx = Vertx.vertx();
        final GrpcIoServer server = GrpcIoServer.server(vertx);

        final PublishBuildEventGrpc.PublishBuildEventImplBase service = new PublishBuildEventGrpc.PublishBuildEventImplBase()
        {
            @Override
            public void publishLifecycleEvent(final PublishLifecycleEventRequest request, final StreamObserver<Empty> responseObserver)
            {
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
                LOGGER.debug("BuildEvent {}", request.getBuildEvent());
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
                        LOGGER.debug("StreamBuildEvent {}", request.getOrderedBuildEvent());
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
        };

        GrpcIoServiceBridge.bridge(service).bind(server);

        vertx.createHttpServer()
                .requestHandler(server)
                .listen(8080)
                .onComplete(httpServer -> {
                            LOGGER.info("HTTP server listening on port {}", httpServer.actualPort());
                            Runtime.getRuntime().addShutdownHook(new Thread(httpServer::close));
                        },
                        err -> {
                            LOGGER.error("HTTP server failed", err);
                        });
    }
}
