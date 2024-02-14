package nickbreen.bes;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import com.beust.jcommander.converters.URIConverter;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOError;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

public class BesServer implements Runnable
{
    private final int port;
    private final PublishBuildEventProcessor service;

    public BesServer(final int port, final PublishBuildEventProcessor service)
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
        @Parameter(names = {"-p", "--port"}, description = "TCP port to listen on, also system property 'port' or environment variable 'PORT'")
        int port = Optional.ofNullable(System.getProperty("port", System.getenv("PORT"))).map(Integer::parseInt).orElse(8888);;
        @Parameter(names = {"-b", "--binary-journal"}, description = "Path to write a binary journal.", converter = PathConverter.class)
        Path binaryJournal;
        @Parameter(names = {"-j", "--json-journal"}, description = "Path to write a binary journal.", converter = PathConverter.class)
        Path jsonJournal;
        @Parameter(names = {"-t", "--text-journal"}, description = "Path to write a binary journal.", converter = PathConverter.class)
        Path textJournal;
        @Parameter(names = {"-d", "--db", "--database"}, description = "JDBC URL to store JSON documents.", converter = URIConverter.class)
        URI jdbc;
        @Parameter(names = {"-x", "--proxy"}, converter = URIConverter.class)
        URI proxy;
    }

    public static void main(final String[] args)
    {
        final Args parsedArgs = new Args();
        JCommander.newBuilder().addObject(parsedArgs).build().parse(args);

        final PublishBuildEventProcessor service = new PublishBuildEventProcessor.Builder()
                .proxy(parsedArgs.proxy)
                .jdbc(parsedArgs.jdbc)
                .binaryJournal(parsedArgs.binaryJournal)
                .jsonJournal(parsedArgs.jsonJournal)
                .textJournal(parsedArgs.textJournal)
                .build();
        new BesServer(parsedArgs.port, service).run();
    }

}
