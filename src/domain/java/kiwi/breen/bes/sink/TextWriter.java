package kiwi.breen.bes.sink;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;

import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

public class TextWriter implements Consumer<Message>
{
    private final TextFormat.Printer printer;
    private final PrintWriter sink;

    public TextWriter(final TextFormat.Printer printer, final PrintWriter sink)
    {
        this.printer = printer;
        this.sink = sink;
    }

    @Override
    public void accept(final Message message)
    {
        try
        {
            printer.print(message, sink);
            sink.println();
        }
        catch (IOException e)
        {
            throw new IOError(e);
        }
    }
}
