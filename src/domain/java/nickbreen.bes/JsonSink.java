package nickbreen.bes;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import java.io.IOError;
import java.io.IOException;
import java.util.function.Consumer;

public class JsonSink implements Consumer<Message>
{
    private final JsonFormat.Printer printer;
    private final Appendable writer;

    public JsonSink(final JsonFormat.Printer printer, final Appendable writer)
    {
        this.printer = printer;
        this.writer = writer;
    }

    @Override
    public void accept(final Message message)
    {
        try
        {
            printer.appendTo(message, writer);
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }
}
