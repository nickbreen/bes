package nickbreen.bes;

import com.google.protobuf.Message;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.function.Function;
import java.util.function.Predicate;

public class MessageMatcher<T extends Message, U> extends TypeSafeMatcher<T>
{
    public static <T extends Message, U> MessageMatcher<T, U> messageThat(final Class<T> t, final Function<T, U> transform, final Matcher<U> matcher)
    {
        return new MessageMatcher<>(t, transform, matcher);
    }
    public static <T extends Message, U> MessageMatcher<T, U> messageThat(final Class<T> t, final Predicate<T> filter, final Function<T, U> transform, final Matcher<U> matcher)
    {
        return new MessageMatcher<>(t, filter, transform, matcher);
    }

    private final Predicate<T> filter;
    private final Function<T, U> transform;
    private final Matcher<U> matcher;

    private MessageMatcher(final Class<T> t, final Function<T, U> transform, final Matcher<U> matcher)
    {
        this(t, o -> true, transform, matcher);
    }

    private MessageMatcher(final Class<T> t, final Predicate<T> filter, final Function<T, U> transform, final Matcher<U> matcher)
    {
        super(t);
        this.filter = filter;
        this.transform = transform;
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely(final T t)
    {
        return filter.test(t) && matcher.matches(transform.apply(t));
    }

    @Override
    public void describeTo(final Description description)
    {
        description.appendText(" and that ").appendDescriptionOf(matcher);
    }
}
