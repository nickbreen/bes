package nickbreen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.StreamId;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import nickbreen.bes.AnyMatcher;
import nickbreen.bes.processor.BazelBuildEventProcessor;
import nickbreen.bes.processor.BuildEventProcessor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static nickbreen.bes.AnyMatcher.anyThat;
import static nickbreen.bes.MessageMatcher.messageThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class BazelBuildEventProcessorTest
{
    @Test
    public void shouldExtractTheUuidFromABazelBuildStartedEvent()
    {
        final String uuid = UUID.randomUUID().toString();
        final List<Message> actual = new ArrayList<>();
        final BuildEventProcessor processor = new BazelBuildEventProcessor(actual::add);
        final OrderedBuildEvent actualEvent = OrderedBuildEvent.newBuilder()
                .setStreamId(StreamId.newBuilder().setInvocationId(uuid).setComponent(StreamId.BuildComponent.CONTROLLER))
                .setSequenceNumber(1)
                .setEvent(BuildEvent.newBuilder()
                        .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                                .setStarted(BuildEventStreamProtos.BuildStarted.newBuilder().setUuid(uuid))
                                .build())))
                .build();
        processor.accept(actualEvent);
        assertThat("has stream ID for UUID", actual, hasItem(
                messageThat(OrderedBuildEvent.class, OrderedBuildEvent::hasStreamId, OrderedBuildEvent::getStreamId,
                        messageThat(StreamId.class, s -> StreamId.BuildComponent.CONTROLLER.equals(s.getComponent()), StreamId::getInvocationId, is(uuid)))));

        assertThat("has bazel build started event for UUID", actual, hasItem(
                messageThat(OrderedBuildEvent.class, OrderedBuildEvent::hasEvent, OrderedBuildEvent::getEvent,
                        anyThat(BuildEvent.class, BuildEventStreamProtos.BuildEvent.class, BuildEvent::hasBazelEvent, BuildEvent::getBazelEvent,
                                messageThat(BuildEventStreamProtos.BuildEvent.class, BuildEventStreamProtos.BuildEvent::hasStarted, BuildEventStreamProtos.BuildEvent::getStarted,
                                        messageThat(BuildEventStreamProtos.BuildStarted.class, BuildEventStreamProtos.BuildStarted::getUuid,
                                                is(uuid)))))));
    }
}