package kiwi.breen.bes.sink;

import com.google.protobuf.Message;

import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

public class BinaryWriter implements Consumer<Message>
{
    private final OutputStream sink;

    public BinaryWriter(final OutputStream sink)
    {
        this.sink = sink;
    }

    @Override
    public void accept(final Message message)
    {
        try
        {
            message.writeDelimitedTo(sink);
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }
}
