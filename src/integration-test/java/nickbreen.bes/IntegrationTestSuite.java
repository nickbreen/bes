package nickbreen.bes;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({
        BinarySinkTest.class,
        JsonSinkTest.class,
        TextSinkTest.class,
})
@RunWith(Suite.class)
public interface IntegrationTestSuite
{
}
