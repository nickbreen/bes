package nickbreen.bes;

import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.util.function.Consumer;

import static nickbreen.bes.Util.testAndConsume;

class BuildEventConsumer
{
    private final Consumer<Message> logger;
    private final Consumer<Any> anyDelegate;

    public BuildEventConsumer(final Consumer<Message> logger, final Consumer<Any> anyDelegate)
    {
        this.logger = logger;
        this.anyDelegate = anyDelegate;
    }

    public void accept(final PublishBuildToolEventStreamRequest request)
    {
        logger.accept(request);
        testAndConsume(request::hasOrderedBuildEvent, request::getOrderedBuildEvent, this::accept);
    }

    public void accept(final PublishLifecycleEventRequest request)
    {
        logger.accept(request);
        testAndConsume(request::hasBuildEvent, request::getBuildEvent, this::accept);
    }

    private void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        logger.accept(orderedBuildEvent);
        testAndConsume(orderedBuildEvent::hasEvent, orderedBuildEvent::getEvent, this::accept);
    }

    @SuppressWarnings("DuplicatedCode")
    private void accept(final BuildEvent buildEvent)
    {
        testAndConsume(buildEvent::hasInvocationAttemptStarted, buildEvent::getInvocationAttemptStarted, logger);
        testAndConsume(buildEvent::hasInvocationAttemptFinished, buildEvent::getInvocationAttemptFinished, logger);
        testAndConsume(buildEvent::hasBuildEnqueued, buildEvent::getBuildEnqueued, logger);
        testAndConsume(buildEvent::hasBuildFinished, buildEvent::getBuildFinished, logger);
        testAndConsume(buildEvent::hasConsoleOutput, buildEvent::getConsoleOutput, logger);
        testAndConsume(buildEvent::hasComponentStreamFinished, buildEvent::getComponentStreamFinished, logger);
        testAndConsume(buildEvent::hasBuildExecutionEvent, buildEvent::getBuildExecutionEvent, anyDelegate);
        testAndConsume(buildEvent::hasSourceFetchEvent, buildEvent::getSourceFetchEvent, anyDelegate);
        testAndConsume(buildEvent::hasBazelEvent, buildEvent::getBazelEvent, anyDelegate);
    }

}
