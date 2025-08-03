package kiwi.breen.bes;

import kiwi.breen.bes.processor.DatabaseProcessorIntegrationTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuppressWarnings("unused")
@SuiteClasses({
        DatabaseProcessorIntegrationTest.class,
})
@RunWith(Suite.class)
public interface IntegrationTestSuite
{
}
