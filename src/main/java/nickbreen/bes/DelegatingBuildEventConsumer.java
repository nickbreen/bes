package nickbreen.bes;

import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.util.function.Consumer;

import static nickbreen.bes.Util.testAndConsume;

class DelegatingBuildEventConsumer implements BuildEventConsumer
{
    protected final Consumer<Any> anyDelegate;
    private final Consumer<Message> logger;

    public DelegatingBuildEventConsumer(final Consumer<Any> anyDelegate, final Consumer<Message> logger)
    {
        this.anyDelegate = anyDelegate;
        this.logger = logger;
    }

    @Override
    public void accept(final PublishBuildToolEventStreamRequest request)
    {
        logger.accept(request);
        testAndConsume(request::hasOrderedBuildEvent, request::getOrderedBuildEvent, this::accept);
    }

    @Override
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
        testAndConsume(buildEvent::hasInvocationAttemptStarted, buildEvent::getInvocationAttemptStarted, this::accept);
        testAndConsume(buildEvent::hasInvocationAttemptFinished, buildEvent::getInvocationAttemptFinished, this::accept);
        testAndConsume(buildEvent::hasBuildEnqueued, buildEvent::getBuildEnqueued, this::accept);
        testAndConsume(buildEvent::hasBuildFinished, buildEvent::getBuildFinished, this::accept);
        testAndConsume(buildEvent::hasConsoleOutput, buildEvent::getConsoleOutput, this::accept);
        testAndConsume(buildEvent::hasComponentStreamFinished, buildEvent::getComponentStreamFinished, this::accept);
        testAndConsume(buildEvent::hasBuildExecutionEvent, buildEvent::getBuildExecutionEvent, anyDelegate);
        testAndConsume(buildEvent::hasSourceFetchEvent, buildEvent::getSourceFetchEvent, anyDelegate);
        testAndConsume(buildEvent::hasBazelEvent, buildEvent::getBazelEvent, anyDelegate);
    }

    private void accept(final Message message)
    {
        logger.accept(message);
    }
}
