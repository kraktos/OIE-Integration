package code.dws.wordnet;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class SimilatityWebService
{
    public static double getSimScore(String arg1, String arg2) throws Exception
    {
        String response = null;

        String uri = "http://swoogle.umbc.edu/SimService/GetSimilarity?operation=api";

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(uri);

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("phrase1", arg1));
            nameValuePairs.add(new BasicNameValuePair("phrase2", arg2));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse httpResponse = httpclient.execute(httppost);

            HttpEntity httpResponseEntity = httpResponse.getEntity();

            if (httpResponseEntity != null) {
                response = EntityUtils.toString(httpResponseEntity);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (response != null && response.length() > 0) ? Double.valueOf(response) : 0;

    }

}
