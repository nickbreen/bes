package nickbreen.bes;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({
        FixturesCompatibilityTest.class,
        BuildEventSinkProcessorTest.class,
        BazelBuildEventProcessorTest.class,
})
@RunWith(Suite.class)
public class TestSuite
{
}
