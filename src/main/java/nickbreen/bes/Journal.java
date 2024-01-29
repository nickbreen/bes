package nickbreen.bes;

import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;

import java.io.PrintStream;

public class Journal implements BuildEventConsumer
{
    private final PrintStream journal;

    public Journal(final PrintStream journal)
    {
        this.journal = journal;
    }

    public void accept(final PublishBuildToolEventStreamRequest request)
    {
        journal.print(request);
    }

    public void accept(final PublishLifecycleEventRequest request)
    {
        journal.print(request);
    }
}
