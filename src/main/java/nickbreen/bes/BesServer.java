package nickbreen.bes;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IParametersValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterFactory;
import com.beust.jcommander.IStringConverterInstanceFactory;
import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toUnmodifiableList;

public class BesServer implements Runnable
{
    private final int port;
    private final PublishBuildEventService service;

    public BesServer(final int port, final PublishBuildEventService service)
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

    static class Args
    {
        @Parameter(names = {"-p", "--port"}, description = "TCP port to listen on, also system property 'port' or environment variable 'PORT'")
        int port = Optional.ofNullable(System.getProperty("port", System.getenv("PORT"))).map(Integer::parseInt).orElse(8888);;

        @Parameter(description = "destinations", converter = UriConverter.class, validateValueWith = UriValidator.class)
        List<URI> destinations;

        static class UriConverter implements IStringConverter<URI>
        {
            @Override
            public URI convert(final String s)
            {
                try
                {
                    return new URI(s);
                }
                catch (URISyntaxException e)
                {
                    throw new ParameterException(e);
                }
            }
        }

        static class UriValidator implements IValueValidator<URI>
        {

            @Override
            public void validate(final String s, final URI uri) throws ParameterException
            {
                if (!uri.isAbsolute())
                {
                    throw new ParameterException("Destinations must be absolute URI's");
                }
            }
        }
    }

    public static void main(final String[] args)
    {
        final Args parsedArgs = new Args();
        JCommander.newBuilder().addObject(parsedArgs).build().parse(args);

        final PublishBuildEventService service = parsedArgs.destinations.stream()
                .map(ProcessorFactory::create)
                .collect(collectingAndThen(toUnmodifiableList(), PublishBuildEventService::new));
        new BesServer(parsedArgs.port, service).run();
    }
}
