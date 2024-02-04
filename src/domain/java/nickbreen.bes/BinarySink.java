package nickbreen.bes;

import com.google.protobuf.Message;

import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

class BinarySink implements Consumer<Message>
{
    private final OutputStream out;

    public BinarySink(final OutputStream out)
    {
        this.out = out;
    }

    @Override
    public void accept(final Message message)
    {
        try
        {
            message.writeDelimitedTo(out);
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }
}
