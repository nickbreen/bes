package nickbreen.bes;

import com.google.devtools.build.v1.PublishBuildToolEventStreamRequest;
import com.google.devtools.build.v1.PublishLifecycleEventRequest;

public interface PublishEventProcessor
{
    void accept(PublishBuildToolEventStreamRequest request);
    void accept(PublishLifecycleEventRequest request);
}
