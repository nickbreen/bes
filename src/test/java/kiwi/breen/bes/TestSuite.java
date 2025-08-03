package kiwi.breen.bes;

import kiwi.breen.bes.processor.BazelBuildEventProcessorTest;
import kiwi.breen.bes.processor.BinaryJournalProcessorTest;
import kiwi.breen.bes.processor.JsonlJournalProcessorTest;
import kiwi.breen.bes.processor.TextJournalProcessorTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuppressWarnings("unused")
@SuiteClasses({
        FixturesCompatibilityTest.class,
        BazelBuildEventProcessorTest.class,
        BinaryJournalProcessorTest.class,
        JsonlJournalProcessorTest.class,
        TextJournalProcessorTest.class,
})
@RunWith(Suite.class)
public interface TestSuite
{
}
