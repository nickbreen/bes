package kiwi.breen.bes;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.function.Function;
import java.util.function.Predicate;

public class AnyMatcher<T extends Message, U extends Message> extends TypeSafeMatcher<T>
{
    public static <T extends Message, U extends Message> AnyMatcher<T,U> anyThat(final Class<T> t, final Class<U> any, final Predicate<T> filter, final Function<T, Any> transform, final Matcher<U> matcher)
    {
        return new AnyMatcher<>(t, any, filter, transform, matcher);
    }

    private final Class<U> any;
    private final Predicate<T> filter;
    private final Function<T, Any> transform;
    private final Matcher<U> matcher;

    private AnyMatcher(final Class<T> t, final Class<U> any, final Predicate<T> filter, final Function<T, Any> transform, final Matcher<U> matcher)
    {
        super(t);
        this.any = any;
        this.filter = filter;
        this.transform = transform;
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely(final T t)
    {
        return filter.test(t) && Util.unpack(this.any, transform.apply(t)).map(matcher::matches).orElse(false);
    }

    @Override
    public void describeTo(final Description description)
    {
        description.appendText(" with an Any of type ").appendValue(any.getCanonicalName()).appendText(" that ").appendDescriptionOf(matcher);
    }
}
