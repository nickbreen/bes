package nickbreen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.Message;

import java.util.function.Consumer;

public class JournalProcessor extends BuildEventProcessor
{
    protected final Consumer<Message> sink;

    public JournalProcessor(final Consumer<Message> sink)
    {
        this.sink = sink;
    }

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        sink.accept(orderedBuildEvent);
    }
}
