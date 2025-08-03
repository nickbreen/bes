package kiwi.breen.bes.processor;

public class JournalException extends RuntimeException
{
    public JournalException(final String message, final Exception cause)
    {
        super(message, cause);
    }
}
