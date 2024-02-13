package nickbreen.bes.args;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

import java.net.URI;

public class UriValidator implements IValueValidator<URI>
{

    @Override
    public void validate(final String s, final URI uri) throws ParameterException
    {
        if (!uri.isAbsolute())
        {
            throw new ParameterException("Destinations must be absolute URI's");
        }
    }
}
