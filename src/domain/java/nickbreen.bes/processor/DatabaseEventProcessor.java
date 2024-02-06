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
import java.util.Properties;

public class DatabaseEventProcessor extends BuildEventProcessor
{
    private final DataSource dataSource;
    private final JsonFormat.Printer printer;
    private final String create;
    private final String insert;

    public DatabaseEventProcessor(final DataSource dataSource, final JsonFormat.Printer printer, final Properties properties)
    {
        this.dataSource = dataSource;
        this.printer = printer;
        this.create = properties.getProperty("sql.create");
        this.insert = properties.getProperty("sql.insert");
    }

    public DatabaseEventProcessor init()
    {
        try (final Connection connection = dataSource.getConnection())
        {
            try (final Statement statement = connection.createStatement())
            {
                statement.execute(create);
            }
        }
        catch (SQLException e)
        {
            throw new Error(e);
        }
        return this;
    }

    @Override
    protected void accept(final OrderedBuildEvent orderedBuildEvent)
    {
        try (final Connection connection = dataSource.getConnection())
        {
            try (final PreparedStatement insert = connection.prepareStatement(this.insert))
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
}
