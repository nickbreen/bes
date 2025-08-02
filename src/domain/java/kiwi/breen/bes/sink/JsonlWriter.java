package kiwi.breen.bes.sink;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class JsonlWriter implements Consumer<Message>
{
    private final JsonFormat.Printer printer;
    private final PrintWriter sink;

    public JsonlWriter(final JsonFormat.Printer printer, final PrintWriter sink)
    {
        this.printer = printer;
        this.sink = sink;
    }

    @Override
    public void accept(final Message message)
    {
        try
        {
            printer.appendTo(message, sink);
            sink.println();
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }
}
