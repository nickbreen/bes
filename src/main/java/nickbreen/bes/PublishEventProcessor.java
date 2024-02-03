package nickbreen.bes;

import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Message;

import java.io.*;
import java.net.URI;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface PublishEventProcessor
{
    void accept(PublishBuildToolEventStreamRequest request);

    void accept(PublishLifecycleEventRequest request);

    interface Factory
    {
        static PublishEventProcessor create(final URI uri)
        {
            return switch (uri.getScheme())
            {
                case "bazel" ->
                {
                    final URI sinkUri = URI.create(uri.getSchemeSpecificPart());
                    final BiConsumer<Message, OutputStream> writer = (message, outputStream) -> {
                        try
                        {
                            outputStream.write(message.toString().getBytes());
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                    };
                    final Consumer<Message> sink = createSink(sinkUri, writer);
                    yield new BazelBuildEventProcessor(sink);
                }
                case "journal" ->
                {
                    final URI sinkUri = URI.create(uri.getSchemeSpecificPart());
                    final BiConsumer<Message, OutputStream> writer = (message, outputStream) -> {
                        try
                        {
                            message.writeDelimitedTo(outputStream);
                        }
                        catch (final IOException e)
                        {
                            throw new IOError(e);
                        }
                    };
                    final Consumer<Message> sink = createSink(sinkUri, writer);
                    yield new BuildEventSinkProcessor(sink);
                }
                default -> throw new Error("Unknown scheme " + uri);
            };
        }

        private static Consumer<Message> createSink(final URI uri, final BiConsumer<Message, OutputStream> writer)
        {
            return switch (uri.getScheme())
            {
                case "file":
                    yield createFile(uri, writer);
                case "jdbc":
                    yield createJdbc(uri);
                case "redis":
                    yield createRedis(uri);
                default:
                    throw new Error("Unknown scheme " + uri);
            };
        }

        private static Consumer<Message> createJdbc(final URI uri)
        {
            throw new Error("JDBC not supported yet " + uri);
        }

        private static Consumer<Message> createRedis(final URI uri)
        {
            throw new Error("Redis not supported yet " + uri);
        }

        private static Consumer<Message> createFile(final URI uri, final BiConsumer<Message, OutputStream> writer)
        {
            try
            {
                @SuppressWarnings("resource") // it is used in the lambda, so we cannot try-with-resources
                final OutputStream outputStream = new FileOutputStream(new File(uri));
                return message -> writer.accept(message, outputStream);
            }
            catch (FileNotFoundException e)
            {
                throw new IOError(e);
            }
        }
    }
}
