package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEventProto;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface Util
{
    static <T> void testAndConsume(final BooleanSupplier test, final Supplier<T> supply, final Consumer<T> consume)
    {
        if (test.getAsBoolean())
        {
            consume.accept(supply.get());
        }
    }

    static <T extends Message> Optional<T> unpack(final Class<T> type, final Any any)
    {
        try
        {
            if (any.is(type))
            {
                return Optional.of(any.unpack(type));
            }
        }
        catch (final InvalidProtocolBufferException e)
        {
            throw new Error(e);
        }
        return Optional.empty();
    }

    static <T extends Message> void unpackAndConsume(final Class<T> type, final Any any, final Consumer<T> consume)
    {
        try
        {
            if (any.is(type))
            {
                consume.accept(any.unpack(type));
            }
        }
        catch (final InvalidProtocolBufferException e)
        {
            throw new Error(e);
        }
    }

    static <T> Stream<T> parseBinary(final ParseDelimitedFrom<T> parseDelimitedFrom, final InputStream bes) throws IOException
    {
        final Stream.Builder<T> events = Stream.builder();
        for (T message = parseDelimitedFrom.parseDelimitedFrom(bes); null != message; message = parseDelimitedFrom.parseDelimitedFrom(bes))
        {
            events.add(message);
        }
        return events.build();
    }

    static JsonFormat.Printer buildJsonPrinter()
    {
        return JsonFormat.printer().usingTypeRegistry(buildTypeRegistry()).omittingInsignificantWhitespace();
    }

    static JsonFormat.Parser buildJsonParser()
    {
        return JsonFormat.parser().usingTypeRegistry(buildTypeRegistry());
    }

    static TextFormat.Printer buildTextPrinter()
    {
        return TextFormat.printer().usingTypeRegistry(buildTypeRegistry());
    }

    static TypeRegistry buildTypeRegistry()
    {
        return TypeRegistry.newBuilder()
                .add(BuildEventProto.getDescriptor().getMessageTypes())
                .add(BuildEventStreamProtos.getDescriptor().getMessageTypes())
                .build();
    }

    @FunctionalInterface
    interface ParseDelimitedFrom<T>
    {
        T parseDelimitedFrom(InputStream is) throws IOException;
    }

    static Stream<OrderedBuildEvent> parseDelimitedJson(final OrderedBuildEvent.Builder builder, final InputStream bes)
    {
        final JsonFormat.Parser parser = Util.buildJsonParser();
        try (final BufferedReader r = new BufferedReader(new InputStreamReader(bes)))
        {
            return r.lines().map(json -> {
                try
                {
                    parser.merge(json, builder.clear());
                }
                catch (InvalidProtocolBufferException e)
                {
                    throw new Error(e);
                }
                return builder.build();
            });
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }
}
