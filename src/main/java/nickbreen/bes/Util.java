package nickbreen.bes;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Util
{
    static <T> void testAndConsume(final BooleanSupplier test, final Supplier<T> supply, final Consumer<T> consume)
    {
        if (test.getAsBoolean())
        {
            consume.accept(supply.get());
        }
    }

    static <T extends Message> void unpackAndConsume(final Class<T> type, final Any any, final Consumer<T> consume)
    {
        try
        {
            if (any.is(type))
            {
                consume.accept(any.unpack(type));
            }
        } catch (final InvalidProtocolBufferException e)
        {
            throw new RuntimeException(e);
        }
    }
}
