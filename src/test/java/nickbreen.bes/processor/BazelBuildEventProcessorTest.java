package nickbreen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEvent;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import nickbreen.bes.processor.BazelBuildEventProcessor;
import nickbreen.bes.processor.BuildEventProcessor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;

public class BazelBuildEventProcessorTest
{
    @Test
    public void shouldExtractTheUuidFromABazelBuildStartedEvent()
    {
        final String uuid = UUID.randomUUID().toString();
        final List<Message> actual = new ArrayList<>();
        final BuildEventProcessor processor = new BazelBuildEventProcessor(actual::add);
        final BuildEvent actualEvent = BuildEvent.newBuilder()
                .setBazelEvent(Any.pack(BuildEventStreamProtos.BuildEvent.newBuilder()
                        .setStarted(BuildEventStreamProtos.BuildStarted.newBuilder().setUuid(uuid))
                        .build()))
                .build();
        processor.accept(actualEvent);
        assertThat("has bazel build started event for UUID", actual, hasItem(hasProperty("uuid", equalTo(uuid))));
    }
}