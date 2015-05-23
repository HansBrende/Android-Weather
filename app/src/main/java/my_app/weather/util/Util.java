package my_app.weather.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by Hans on 5/15/2015.
 */
public abstract class Util {
    private Util() {}

    public static final String TAG = "WEATHER";
    public static final String CHARSET_NAME = "UTF-8";
    private static final SAXParserFactory spf = SAXParserFactory.newInstance();
    static {
        spf.setNamespaceAware(true);
    }

    public static void parseXml(InputStream str, DefaultHandler handler) throws ParserConfigurationException, SAXException, IOException {
        spf.newSAXParser().parse(str, handler);
    }

    public static String urlEncode(String url) {
        try {
            return URLEncoder.encode(url, CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e); //should never happen
        }
    }

    public static boolean isBlank(String s) {
        return s == null || s.isEmpty() || s.equalsIgnoreCase("null");
    }

    public static String convertStreamToString(java.io.InputStream is) throws IOException {
        java.util.Scanner s = new java.util.Scanner(is, CHARSET_NAME).useDelimiter("\\A");
        String string = s.hasNext() ? s.next() : "";
        IOException ioe = s.ioException();
        s.close();
        if (ioe != null)
            throw ioe;
        return string;
    }

    public static URL forecastURL(long woeid, boolean f) {
        try {
            return new URL("http://weather.yahooapis.com/forecastrss?w=" + woeid + "&u=" + (f ? 'f' : 'c'));
        } catch (MalformedURLException e) {
            throw new Error(e); //should never happen
        }
    }

    //get rid of commas to improve accuracy of search results
    //e.g. "Troy, IL" returns Troy, NY, but "Troy IL" returns Troy, IL
    private static String sanitize(String query) {
        return query.replace('"', ' ').replace(',', ' ');
    }

    public static long calculateWoeid(String location) throws IOException {
        return queryWoeid("select woeid from geo.places(1) where text=\"" + sanitize(location) + "\"");
    }

    public static long calculateWoeid(double lat, double lon) throws IOException {
        return queryWoeid("select woeid from geo.placefinder where text=\"" + lat + "," + lon + "\" and gflags=\"R\"");
    }

    private static long queryWoeid(String query) throws IOException {
        String url = "http://query.yahooapis.com/v1/public/yql?format=xml&q=" + urlEncode(query);
        InputStream stream = new URL(url).openStream();
        final StringBuilder characters = new StringBuilder();
        try {
            parseXml(stream, new DefaultHandler() {
                boolean appendChars = false;

                public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                    appendChars = characters.length() == 0 && localName.equalsIgnoreCase("woeid");
                }

                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (appendChars)
                        characters.append(ch, start, length);
                }

                public void endElement(String uri, String localName, String qName) throws SAXException {
                    appendChars = false;
                }
            });
        } catch (SAXException e) {
            return -1L;
        } catch (ParserConfigurationException e) {
            return -1L;
        } finally {
            stream.close();
        }
        try {
            return Long.parseLong(characters.toString().trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
