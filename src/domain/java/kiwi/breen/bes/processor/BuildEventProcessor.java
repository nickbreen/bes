package kiwi.breen.bes.processor;

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.v1.BuildEventProto;
import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;
import com.google.protobuf.TypeRegistry;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    protected static <T> void testAndConsume(
            final BooleanSupplier test,
            final Supplier<T> supply,
            final Consumer<T> consume)
    {
        if (test.getAsBoolean())
        {
            consume.accept(supply.get());
        }
    }

    public static TypeRegistry buildTypeRegistry()
    {
        return TypeRegistry.newBuilder()
                .add(BuildEventProto.getDescriptor().getMessageTypes())
                .add(BuildEventStreamProtos.getDescriptor().getMessageTypes())
                .build();
    }
}
