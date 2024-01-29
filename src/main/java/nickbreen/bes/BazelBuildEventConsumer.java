package nickbreen.bes;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import java.util.function.Consumer;

import static nickbreen.bes.Util.testAndConsume;
import static nickbreen.bes.Util.unpackAndConsume;

public class BazelBuildEventConsumer implements Consumer<Any>
{
    private final Consumer<Message> consumer;

    public BazelBuildEventConsumer(final Consumer<Message> consumer)
    {
        this.consumer = consumer;
    }

    @Override
    public void accept(final Any any)
    {
        unpackAndConsume(BuildEventStreamProtos.BuildEvent.class, any, this::accept);
        unpackAndConsume(BuildEventStreamProtos.BuildEventId.class, any, consumer::accept);
        unpackAndConsume(BuildEventStreamProtos.BuildFinished.class, any, consumer::accept);
        unpackAndConsume(BuildEventStreamProtos.BuildMetadata.class, any, consumer::accept);
    }

    @SuppressWarnings("DuplicatedCode")
    private void accept(final BuildEventStreamProtos.BuildEvent buildEvent)
    {
        testAndConsume(buildEvent::hasAborted, buildEvent::getAborted, consumer);
        testAndConsume(buildEvent::hasAction, buildEvent::getAction, consumer);
        testAndConsume(buildEvent::hasBuildMetadata, buildEvent::getBuildMetadata, consumer);
        testAndConsume(buildEvent::hasBuildMetrics, buildEvent::getBuildMetrics, consumer);
        testAndConsume(buildEvent::hasBuildToolLogs, buildEvent::getBuildToolLogs, consumer);
        testAndConsume(buildEvent::hasCompleted, buildEvent::getCompleted, consumer);
        testAndConsume(buildEvent::hasConfiguration, buildEvent::getConfiguration, consumer);
        testAndConsume(buildEvent::hasConfigured, buildEvent::getConfigured, consumer);
        testAndConsume(buildEvent::hasConvenienceSymlinksIdentified, buildEvent::getConvenienceSymlinksIdentified, consumer);
        testAndConsume(buildEvent::hasExpanded, buildEvent::getExpanded, consumer);
        testAndConsume(buildEvent::hasFetch, buildEvent::getFetch, consumer);
        testAndConsume(buildEvent::hasFinished, buildEvent::getFinished, consumer);
        testAndConsume(buildEvent::hasId, buildEvent::getId, consumer);
        testAndConsume(buildEvent::hasNamedSetOfFiles, buildEvent::getNamedSetOfFiles, consumer);
        testAndConsume(buildEvent::hasOptionsParsed, buildEvent::getOptionsParsed, consumer);
        testAndConsume(buildEvent::hasProgress, buildEvent::getProgress, consumer);
        testAndConsume(buildEvent::hasStarted, buildEvent::getStarted, consumer);
//        demux(buildEvent::hasStructuredCommandLine, buildEvent::getStructuredCommandLine, consumer);
        testAndConsume(buildEvent::hasTargetSummary, buildEvent::getTargetSummary, consumer);
        testAndConsume(buildEvent::hasTestResult, buildEvent::getTestResult, consumer);
        testAndConsume(buildEvent::hasTestSummary, buildEvent::getTestResult, consumer);
        testAndConsume(buildEvent::hasUnstructuredCommandLine, buildEvent::getUnstructuredCommandLine, consumer);
        testAndConsume(buildEvent::hasWorkspaceInfo, buildEvent::getWorkspaceInfo, consumer);
        testAndConsume(buildEvent::hasWorkspaceStatus, buildEvent::getWorkspaceStatus, consumer);
        testAndConsume(buildEvent::hasWorkspaceStatus, buildEvent::getWorkspaceStatus, consumer);
    }
}
