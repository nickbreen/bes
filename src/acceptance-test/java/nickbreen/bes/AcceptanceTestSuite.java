package nickbreen.bes;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({
        AcceptanceTest.class,
        ContainerAcceptanceTest.class,
})
@RunWith(Suite.class)
public interface AcceptanceTestSuite
{
}
