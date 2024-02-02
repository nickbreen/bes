package nickbreen.bes;

import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;

import static nickbreen.bes.Util.testAndConsume;

public abstract class BaseBuildEventConsumer
{
    public final void accept(final PublishBuildToolEventStreamRequest request)
    {
        testAndConsume(request::hasOrderedBuildEvent, request::getOrderedBuildEvent, this::accept);
    }

    public final void accept(final PublishLifecycleEventRequest request)
    {
        testAndConsume(request::hasBuildEvent, request::getBuildEvent, this::accept);
    }

    private void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        testAndConsume(orderedBuildEvent::hasEvent, orderedBuildEvent::getEvent, this::accept);
    }

    protected abstract void accept(final BuildEvent buildEvent);
}
