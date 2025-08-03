package kiwi.breen.bes;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import com.beust.jcommander.converters.URIConverter;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import kiwi.breen.bes.processor.BinaryJournalProcessor;
import kiwi.breen.bes.processor.DatabaseProcessor;
import kiwi.breen.bes.processor.JsonlJournalProcessor;
import kiwi.breen.bes.processor.PublishEventProcessor;
import kiwi.breen.bes.processor.TextJournalProcessor;

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public class BesServer implements Runnable
{
    private final int port;
    private final BindableService service;

    public BesServer(final int port, final BindableService service)
    {
        this.port = port;
        this.service = service;
    }

    @Override
    public void run()
    {
        final Server server = ServerBuilder.forPort(port).addService(service).build();

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

        try
        {
            server.start().awaitTermination();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }

    private static class Args
    {
        @Parameter(
                names = {"-p", "--port"},
                description = "TCP port to listen on, also system property 'port' or environment variable 'PORT'")
        int port = Optional.ofNullable(System.getProperty("port", System.getenv("PORT")))
                .map(Integer::parseInt).orElse(8888);
        @Parameter(
                names = {"-b", "--binary-journal"},
                description = "Path to write a binary journal.",
                converter = PathConverter.class)
        Path binaryJournal;
        @Parameter(
                names = {"-j", "--json-journal"},
                description = "Path to write a binary journal.",
                converter = PathConverter.class)
        Path jsonJournal;
        @Parameter(
                names = {"-t", "--text-journal"},
                description = "Path to write a binary journal.",
                converter = PathConverter.class)
        Path textJournal;
        @Parameter(
                names = {"-d", "--db", "--database"},
                description = "JDBC URL to store JSON documents.",
                converter = URIConverter.class)
        URI jdbc;
    }

    public static void main(final String[] args)
    {
        final Args parsedArgs = new Args();
        JCommander.newBuilder().addObject(parsedArgs).build().parse(args);

        final BindableService service = new Builder()
                .jdbc(parsedArgs.jdbc)
                .binaryJournal(parsedArgs.binaryJournal)
                .jsonJournal(parsedArgs.jsonJournal)
                .textJournal(parsedArgs.textJournal)
                .build();

        new BesServer(parsedArgs.port, service).run();
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
                    .map(DatabaseProcessor::create);
            return this;
        }

        public Builder binaryJournal(final Path path)
        {
            return binaryJournal(Optional.ofNullable(path));
        }

        private Builder binaryJournal(final Optional<Path> path)
        {
            this.binaryJournal = path.map(BinaryJournalProcessor::create);
            return this;
        }

        public Builder jsonJournal(final Path path)
        {
            return jsonJournal(Optional.ofNullable(path));
        }

        public Builder jsonJournal(final Optional<Path> path)
        {
            this.jsonJournal = path.map(JsonlJournalProcessor::create);
            return this;
        }

        public Builder textJournal(final Path path)
        {
            return textJournal(Optional.ofNullable(path));
        }

        public Builder textJournal(final Optional<Path> path)
        {
            this.textJournal = path.map(TextJournalProcessor::create);
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
