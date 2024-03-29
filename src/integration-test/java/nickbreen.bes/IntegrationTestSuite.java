package nickbreen.bes;

import nickbreen.bes.processor.DatabaseProcessorIntegrationTest;
import nickbreen.bes.sink.BinarySinkIntegrationTest;
import nickbreen.bes.sink.JsonSinkIntegrationTest;
import nickbreen.bes.sink.TextSinkIntegrationTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({
        BinarySinkIntegrationTest.class,
        JsonSinkIntegrationTest.class,
        TextSinkIntegrationTest.class,
        DatabaseProcessorIntegrationTest.class,
})
@RunWith(Suite.class)
public interface IntegrationTestSuite
{
}
