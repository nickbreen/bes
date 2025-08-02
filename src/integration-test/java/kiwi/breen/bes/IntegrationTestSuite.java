package kiwi.breen.bes;

import kiwi.breen.bes.processor.DatabaseProcessorIntegrationTest;
import kiwi.breen.bes.sink.BinarySinkIntegrationTest;
import kiwi.breen.bes.sink.JsonSinkIntegrationTest;
import kiwi.breen.bes.sink.TextSinkIntegrationTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuppressWarnings("unused")
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
