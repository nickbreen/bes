package kiwi.breen.bes;

import com.google.devtools.build.v1.OrderedBuildEvent;
import io.grpc.BindableService;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumerOptions;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.grpcio.server.GrpcIoServer;
import io.vertx.grpcio.server.GrpcIoServiceBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VertxMain
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VertxMain.class);

    private enum Address
    {
        obe();
    }

    public static void main(String[] args)
    {
        final Vertx vertx = Vertx.vertx();

        vertx.eventBus().registerDefaultCodec(
                OrderedBuildEvent.class,
                new GenericLocalCodec<>(OrderedBuildEvent.class));

        vertx.eventBus().consumer(Address.obe.name()).handler(
                obe -> LOGGER.debug("OBE\n{}", obe.body()));

        final MessageProducer<OrderedBuildEvent> sender = vertx.eventBus().sender(Address.obe.name());
        final BindableService service = new OrderedBuildEventService(sender::write);
        final GrpcIoServer server = GrpcIoServer.server(vertx);
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
