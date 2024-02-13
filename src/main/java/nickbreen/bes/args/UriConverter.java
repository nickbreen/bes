package nickbreen.bes.args;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

import java.net.URI;
import java.net.URISyntaxException;

public class UriConverter implements IStringConverter<URI>
{
    @Override
    public URI convert(final String s)
    {
        try
        {
            return new URI(s);
        }
        catch (URISyntaxException e)
        {
            throw new ParameterException(e);
        }
    }
}
