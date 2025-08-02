package kiwi.breen.bes.processor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static kiwi.breen.bes.Util.buildJsonPrinter;

public class DatabaseProcessorFactory
{
    public static DatabaseProcessor create(final DataSource ds)
    {
        try (final Connection connection = ds.getConnection())
        {
            final String sql = switch (connection.getMetaData().getDatabaseProductName().toLowerCase())
            {
                case "h2" -> "INSERT INTO event VALUES (?,?,?,?,? FORMAT JSON)";
                case "sqlite" -> "INSERT INTO event VALUES (?,?,?,?,JSONB(?)) ON CONFLICT DO NOTHING";
                case "postgresql" -> "INSERT INTO event VALUES (?,?,?,?,CAST(? AS JSONB)) ON CONFLICT DO NOTHING";
                default -> "INSERT IGNORE INTO event VALUES (?,?,?,?,?)";
            };
            return new DatabaseProcessor(buildJsonPrinter(), ds, sql);
        }
        catch (final SQLException e)
        {
            throw new DatabaseProcessorFactoryException(e);
        }
    }
}
