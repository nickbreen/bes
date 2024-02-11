package nickbreen.bes;

import nickbreen.bes.processor.BazelBuildEventProcessorTest;
import nickbreen.bes.processor.JournalProcessorTest;
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
