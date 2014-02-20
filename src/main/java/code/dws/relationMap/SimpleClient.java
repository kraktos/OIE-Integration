package code.dws.relationMap;

import java.io.IOException;

public class SimpleClient
{

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {

        for (int i = 0; i < 20; i++) {
            PropertyStatistics.main(new String[] {String.valueOf(i)});
        }
    }
}
