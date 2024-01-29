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
    private final Consumer<Any> anyDelegate;
    private final Consumer<Message> delegate;

    public DelegatingBuildEventConsumer(final Consumer<Any> anyDelegate, final Consumer<Message> delegate)
    {
        this.anyDelegate = anyDelegate;
        this.delegate = delegate;
    }

    @Override
    public void accept(final PublishBuildToolEventStreamRequest request)
    {
        delegate.accept(request);
        testAndConsume(request::hasOrderedBuildEvent, request::getOrderedBuildEvent, this::accept);
    }

    @Override
    public void accept(final PublishLifecycleEventRequest request)
    {
        delegate.accept(request);
        testAndConsume(request::hasBuildEvent, request::getBuildEvent, this::accept);
    }

    private void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        delegate.accept(orderedBuildEvent);
        testAndConsume(orderedBuildEvent::hasEvent, orderedBuildEvent::getEvent, this::accept);
    }

    @SuppressWarnings("DuplicatedCode")
    private void accept(final BuildEvent buildEvent)
    {
        testAndConsume(buildEvent::hasInvocationAttemptStarted, buildEvent::getInvocationAttemptStarted, delegate);
        testAndConsume(buildEvent::hasInvocationAttemptFinished, buildEvent::getInvocationAttemptFinished, delegate);
        testAndConsume(buildEvent::hasBuildEnqueued, buildEvent::getBuildEnqueued, delegate);
        testAndConsume(buildEvent::hasBuildFinished, buildEvent::getBuildFinished, delegate);
        testAndConsume(buildEvent::hasConsoleOutput, buildEvent::getConsoleOutput, delegate);
        testAndConsume(buildEvent::hasComponentStreamFinished, buildEvent::getComponentStreamFinished, delegate);
        testAndConsume(buildEvent::hasBuildExecutionEvent, buildEvent::getBuildExecutionEvent, anyDelegate);
        testAndConsume(buildEvent::hasSourceFetchEvent, buildEvent::getSourceFetchEvent, anyDelegate);
        testAndConsume(buildEvent::hasBazelEvent, buildEvent::getBazelEvent, anyDelegate);
    }
}
