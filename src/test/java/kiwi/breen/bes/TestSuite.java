package kiwi.breen.bes;

import kiwi.breen.bes.processor.BazelBuildEventProcessorTest;
import kiwi.breen.bes.processor.JournalProcessorTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({
        FixturesCompatibilityTest.class,
        JournalProcessorTest.class,
        BazelBuildEventProcessorTest.class,
})
@RunWith(Suite.class)
public interface TestSuite
{
}
