package nickbreen.bes.sink;

import com.google.protobuf.Message;

import java.io.IOError;
import java.io.IOException;
import java.util.function.Consumer;

public class TextSink implements Consumer<Message>
{
    private final Appendable writer;

    public TextSink(final Appendable writer)
    {
        this.writer = writer;
    }

    @Override
    public void accept(final Message message)
    {
        try
        {
            writer.append(message.toString());
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }
}
