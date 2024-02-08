package nickbreen.bes.data;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EventDAO
{
    public record Event(String buildId, int component, String invocationId, long sequence, String event)
    {
    }

    private final DataSource ds;

    public EventDAO(final DataSource ds)
    {
        this.ds = ds;
    }

    public void insert(final Event event)
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
            try (final PreparedStatement insert = connection.prepareStatement(sql))
            {
                insert.setString(1, event.buildId);
                insert.setInt(2, event.component);
                insert.setString(3, event.invocationId);
                insert.setLong(4, event.sequence);
                insert.setString(5, event.event);
                insert.execute();
            }
        }
        catch (SQLException e)
        {
            throw new Error(e);
        }
    }
}
