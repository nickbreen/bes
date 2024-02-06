package nickbreen.bes.processor;

import com.google.devtools.build.v1.OrderedBuildEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import javax.sql.DataSource;
import java.io.IOError;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseEventProcessor extends BuildEventProcessor
{
    public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS event (build_id TEXT, component INTEGER, invocation_id TEXT, sequence INTEGER, event TEXT, PRIMARY KEY (build_id, component, sequence, invocation_id))";
    private final DataSource dataSource;
    private final JsonFormat.Printer printer;

    public DatabaseEventProcessor(final DataSource dataSource, final JsonFormat.Printer printer)
    {
        this.dataSource = dataSource;
        this.printer = printer;
        initDb(dataSource);
    }

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        try (final Connection connection = dataSource.getConnection())
        {
            try (final PreparedStatement insert = connection.prepareStatement("INSERT INTO event VALUES (?,?,?,?,jsonb(?)) ON CONFLICT DO NOTHING"))
            {
                insert.setString(1, orderedBuildEvent.getStreamId().getBuildId());
                insert.setInt(2, orderedBuildEvent.getStreamId().getComponentValue());
                insert.setString(3, orderedBuildEvent.getStreamId().getInvocationId());
                insert.setLong(4, orderedBuildEvent.getSequenceNumber());
                insert.setString(5, printer.print(orderedBuildEvent.getEvent()));
                insert.execute();
            }
            catch (InvalidProtocolBufferException e)
            {
                throw new IOError(e);
            }
        }
        catch (SQLException e)
        {
            throw new Error(e);
        }
    }

    private static void initDb(final DataSource dataSource)
    {
        try (final Connection connection = dataSource.getConnection())
        {
            try (final Statement statement = connection.createStatement())
            {
                statement.execute(CREATE_TABLE);
            }
        }
        catch (SQLException e)
        {
            throw new Error(e);
        }
    }
}
