package nickbreen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.util.function.Consumer;

import static nickbreen.bes.Util.testAndConsume;
import static nickbreen.bes.Util.unpackAndConsume;

public class BazelBuildEventProcessor extends JournalProcessor
{
    public BazelBuildEventProcessor(final Consumer<Message> sink)
    {
        super(sink);
    }

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        testAndConsume(orderedBuildEvent::hasEvent, orderedBuildEvent::getEvent, this::accept);
    }

    private void accept(final BuildEvent buildEvent)
    {
        testAndConsume(buildEvent::hasInvocationAttemptStarted, buildEvent::getInvocationAttemptStarted, sink);
        testAndConsume(buildEvent::hasInvocationAttemptFinished, buildEvent::getInvocationAttemptFinished, sink);
        testAndConsume(buildEvent::hasBuildEnqueued, buildEvent::getBuildEnqueued, sink);
        testAndConsume(buildEvent::hasBuildFinished, buildEvent::getBuildFinished, sink);
        testAndConsume(buildEvent::hasConsoleOutput, buildEvent::getConsoleOutput, sink);
        testAndConsume(buildEvent::hasComponentStreamFinished, buildEvent::getComponentStreamFinished, sink);
        testAndConsume(buildEvent::hasBuildExecutionEvent, buildEvent::getBuildExecutionEvent, this::accept);
        testAndConsume(buildEvent::hasSourceFetchEvent, buildEvent::getSourceFetchEvent, this::accept);
        testAndConsume(buildEvent::hasBazelEvent, buildEvent::getBazelEvent, this::accept);
    }

    private void accept(final Any any)
    {
        unpackAndConsume(BuildEventStreamProtos.BuildEvent.class, any, this::accept);
        unpackAndConsume(BuildEventStreamProtos.BuildEventId.class, any, sink::accept);
        unpackAndConsume(BuildEventStreamProtos.BuildFinished.class, any, sink::accept);
        unpackAndConsume(BuildEventStreamProtos.BuildMetadata.class, any, sink::accept);
    }

    @SuppressWarnings("DuplicatedCode")
    private void accept(final BuildEventStreamProtos.BuildEvent buildEvent)
    {
        testAndConsume(buildEvent::hasAborted, buildEvent::getAborted, sink);
        testAndConsume(buildEvent::hasAction, buildEvent::getAction, sink);
        testAndConsume(buildEvent::hasBuildMetadata, buildEvent::getBuildMetadata, sink);
        testAndConsume(buildEvent::hasBuildMetrics, buildEvent::getBuildMetrics, sink);
        testAndConsume(buildEvent::hasBuildToolLogs, buildEvent::getBuildToolLogs, sink);
        testAndConsume(buildEvent::hasCompleted, buildEvent::getCompleted, sink);
        testAndConsume(buildEvent::hasConfiguration, buildEvent::getConfiguration, sink);
        testAndConsume(buildEvent::hasConfigured, buildEvent::getConfigured, sink);
        testAndConsume(buildEvent::hasConvenienceSymlinksIdentified, buildEvent::getConvenienceSymlinksIdentified, sink);
        testAndConsume(buildEvent::hasExpanded, buildEvent::getExpanded, sink);
        testAndConsume(buildEvent::hasFetch, buildEvent::getFetch, sink);
        testAndConsume(buildEvent::hasFinished, buildEvent::getFinished, sink);
        testAndConsume(buildEvent::hasId, buildEvent::getId, sink);
        testAndConsume(buildEvent::hasNamedSetOfFiles, buildEvent::getNamedSetOfFiles, sink);
        testAndConsume(buildEvent::hasOptionsParsed, buildEvent::getOptionsParsed, sink);
        testAndConsume(buildEvent::hasProgress, buildEvent::getProgress, sink);
        testAndConsume(buildEvent::hasStarted, buildEvent::getStarted, sink);
        // testAndConsume(buildEvent::hasStructuredCommandLine, buildEvent::getStructuredCommandLine, sink); // requires some runtime.proto
        testAndConsume(buildEvent::hasTargetSummary, buildEvent::getTargetSummary, sink);
        testAndConsume(buildEvent::hasTestResult, buildEvent::getTestResult, sink);
        testAndConsume(buildEvent::hasTestSummary, buildEvent::getTestResult, sink);
        testAndConsume(buildEvent::hasUnstructuredCommandLine, buildEvent::getUnstructuredCommandLine, sink);
        testAndConsume(buildEvent::hasWorkspaceInfo, buildEvent::getWorkspaceInfo, sink);
        testAndConsume(buildEvent::hasWorkspaceStatus, buildEvent::getWorkspaceStatus, sink);
        testAndConsume(buildEvent::hasWorkspaceStatus, buildEvent::getWorkspaceStatus, sink);
    }
}
