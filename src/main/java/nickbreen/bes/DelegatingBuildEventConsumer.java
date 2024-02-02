package nickbreen.bes;

import com.google.devtools.build.v1.BuildEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class DelegatingBuildEventConsumer extends BaseBuildEventConsumer
{
    private final List<Consumer<BuildEvent>> delegates = new ArrayList<>();

    @SafeVarargs
    public static DelegatingBuildEventConsumer create(final Consumer<BuildEvent>... delegates)
    {
        return new DelegatingBuildEventConsumer(List.of(delegates));
    }

    public DelegatingBuildEventConsumer(final List<Consumer<BuildEvent>> delegates)
    {
        this.delegates.addAll(delegates);
    }

    @Override
    protected void accept(final BuildEvent buildEvent)
    {
        delegates.forEach(delegate -> delegate.accept(buildEvent));
    }
}
