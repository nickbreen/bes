package nickbreen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.Message;

import java.util.function.Consumer;

import static nickbreen.bes.Util.testAndConsume;

public class BuildEventSinkProcessor extends BuildEventProcessor
{
    protected final Consumer<Message> sink;

    public BuildEventSinkProcessor(final Consumer<Message> sink)
    {
        this.sink = sink;
    }

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        sink.accept(orderedBuildEvent);
    }
}
