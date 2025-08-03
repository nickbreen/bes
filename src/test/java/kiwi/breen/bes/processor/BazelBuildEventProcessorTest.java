package kiwi.breen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.StreamId;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static kiwi.breen.bes.MessageMatcher.messageThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

public class BazelBuildEventProcessorTest
{
    @Test
    public void shouldExtractTheUuidFromABazelBuildStartedEvent()
    {
        final String uuid = UUID.randomUUID().toString();
        final List<Message> actual = new ArrayList<>();
        final BuildEventProcessor processor = new BazelBuildEventProcessor(actual::add);
        processor.accept(OrderedBuildEvent.newBuilder()
                .setStreamId(StreamId.newBuilder()
                        .setInvocationId(uuid)
                        .setComponent(StreamId.BuildComponent.CONTROLLER))
                .setSequenceNumber(1)
                .setEvent(BuildEvent.newBuilder()
                        .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                                .setStarted(BuildEventStreamProtos.BuildStarted.newBuilder()
                                        .setUuid(uuid))
                                .build())))
                .build());
        processor.accept(OrderedBuildEvent.newBuilder()
                .setStreamId(StreamId.newBuilder()
                        .setInvocationId(uuid)
                        .setComponent(StreamId.BuildComponent.CONTROLLER))
                .setSequenceNumber(1)
                .setEvent(BuildEvent.newBuilder()
                        .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                                .setFinished(BuildEventStreamProtos.BuildFinished.newBuilder()
                                        .setExitCode(BuildEventStreamProtos.BuildFinished.ExitCode.newBuilder()
                                                .setCode(0)))
                                .build())))
                .build());

        assertThat(
                "has bazel build started json for UUID",
                actual,
                hasItem(
                        messageThat(
                                BuildEventStreamProtos.BuildStarted.class,
                                BuildEventStreamProtos.BuildStarted::getUuid,
                                is(uuid))));
    }
}