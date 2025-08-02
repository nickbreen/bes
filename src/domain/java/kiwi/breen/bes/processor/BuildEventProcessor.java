package kiwi.breen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;

import static kiwi.breen.bes.Util.testAndConsume;

public abstract class BuildEventProcessor implements PublishEventProcessor
{
    @Override
    public final void accept(final PublishBuildToolEventStreamRequest request)
    {
        testAndConsume(request::hasOrderedBuildEvent, request::getOrderedBuildEvent, this::accept);
    }

    @Override
    public final void accept(final PublishLifecycleEventRequest request)
    {
        testAndConsume(request::hasBuildEvent, request::getBuildEvent, this::accept);
    }

    protected abstract void accept(final OrderedBuildEvent orderedBuildEvent);
}
