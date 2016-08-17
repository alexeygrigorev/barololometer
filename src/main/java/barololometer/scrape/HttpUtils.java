package barololometer.scrape;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Throwables;

public class HttpUtils {

    private static final String HEADER_USER_AGENT = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:47.0) Gecko/20100101 Firefox/47.0";
    private static final String HEADER_ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

    public static String userAgentRequest(String address) {
        try {
            URL url = new URL(address);
            URLConnection con = url.openConnection();
            con.setRequestProperty("User-Agent", HEADER_USER_AGENT);
            con.setRequestProperty("Accept", HEADER_ACCEPT);
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            try (InputStream is = con.getInputStream()) {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static String encode(String in) {
        try {
            return URLEncoder.encode(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }
}
