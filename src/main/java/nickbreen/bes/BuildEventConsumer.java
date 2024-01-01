package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.PrintStream;

class BuildEventConsumer
{
    private final PrintStream out;

    public BuildEventConsumer(final PrintStream out)
    {
        this.out = out;
    }

    public void accept(final PublishBuildToolEventStreamRequest request)
    {
        on(request);
        demux(request.getOrderedBuildEvent().getEvent());
    }

    public void accept(final PublishLifecycleEventRequest request)
    {
        on(request);
        demux(request.getBuildEvent().getEvent());
    }

    private void demux(final BuildEvent buildEvent)
    {
        if (buildEvent.hasInvocationAttemptStarted())
        {
            on(buildEvent.getInvocationAttemptStarted());
        }
        if (buildEvent.hasInvocationAttemptFinished())
        {
            on(buildEvent.getInvocationAttemptFinished());
        }
        if (buildEvent.hasBuildEnqueued())
        {
            on(buildEvent.getBuildEnqueued());
        }
        if (buildEvent.hasBuildFinished())
        {
            on(buildEvent.getBuildFinished());
        }
        if (buildEvent.hasConsoleOutput())
        {
            on(buildEvent.getConsoleOutput());
        }
        if (buildEvent.hasComponentStreamFinished())
        {
            on(buildEvent.getComponentStreamFinished());
        }
        if (buildEvent.hasBazelEvent())
        {
            on("BazelEvent", buildEvent.getBazelEvent());
            try
            {
                if (buildEvent.getBazelEvent().is(BuildEventStreamProtos.BuildEvent.class))
                {
                    on("BazelEvent", buildEvent.getBazelEvent().unpack(BuildEventStreamProtos.BuildEvent.class));
                }
            } catch (InvalidProtocolBufferException e)
            {
                throw new RuntimeException(e);
            }
        }
        if (buildEvent.hasBuildExecutionEvent())
        {
            on("BuildExecutionEvent", buildEvent.getBuildExecutionEvent());
        }
        if (buildEvent.hasSourceFetchEvent())
        {
            on("SourceFetchEvent", buildEvent.getSourceFetchEvent());
        }
    }

    private void on(final Object arg)
    {
        on(arg.getClass().getSimpleName(), arg);
    }

    private void on(final String what, final Object arg)
    {
        out.printf("*** %s ***:\n%s", what, arg);
    }

    private void on(final String what, final Any any)
    {
        out.printf("*** %s *** %s ***:\n%s", what, any.getTypeUrl(), any);
    }

}
