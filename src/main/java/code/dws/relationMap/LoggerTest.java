package code.dws.relationMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerTest
{

    public static final Logger LOGGER = LoggerFactory.getLogger(LoggerTest.class);

    public static void main(final String[] p_args) throws InterruptedException
    {
        LOGGER.debug("Logger test");
    }
}
