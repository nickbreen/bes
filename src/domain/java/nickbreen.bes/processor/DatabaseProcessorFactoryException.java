package nickbreen.bes.processor;

import java.sql.SQLException;

public class DatabaseProcessorFactoryException extends RuntimeException
{
    public DatabaseProcessorFactoryException(final SQLException e)
    {
        super(e);
    }
}
