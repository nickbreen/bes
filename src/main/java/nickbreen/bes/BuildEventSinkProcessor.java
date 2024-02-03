package nickbreen.bes;

import com.google.devtools.build.v1.BuildEvent;
import com.google.protobuf.Message;

import java.util.function.Consumer;

public class BuildEventSinkProcessor extends BuildEventProcessor
{
    protected final Consumer<Message> sink;

    public BuildEventSinkProcessor(final Consumer<Message> sink)
    {
        this.sink = sink;
    }

    @Override
    protected void accept(final BuildEvent buildEvent)
    {
        sink.accept(buildEvent);
    }
}
