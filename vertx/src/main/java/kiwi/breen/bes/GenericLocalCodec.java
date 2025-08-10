package kiwi.breen.bes;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class GenericLocalCodec<T> implements MessageCodec<T, T>
{
    private final Class<T> t;

    public GenericLocalCodec(final Class<T> t)
    {
        this.t = t;
    }

    @Override
    public void encodeToWire(final Buffer buffer, final T t)
    {
        throw new Error("not implemented");
    }

    @Override
    public T decodeFromWire(final int pos, final Buffer buffer)
    {
        throw new Error("not implemented");
    }

    @Override
    public T transform(final T t)
    {
        return t;
    }

    @Override
    public String name()
    {
        return t.getSimpleName();
    }

    @Override
    public byte systemCodecID()
    {
        return -1;
    }
}
