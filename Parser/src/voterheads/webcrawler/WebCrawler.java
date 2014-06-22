package voterheads.webcrawler;

//A minimal Web Crawler written in Java
//Usage: From command line 
//  java WebCrawler <URL> [N]
//where URL is the url to start the crawl, and N (optional)
//is the maximum number of pages to download.

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import voterheads.FolderManager;
import voterheads.Organization;

public class WebCrawler
{

    public static final int     SEARCH_LIMIT = 20;                                // Absolute
                                                                                   // max
                                                                                   // pages
    public static final boolean DEBUG        = false;
    public static final String  DISALLOW     = "Disallow:";
    public static final int     CRAWL_LIMIT  = 1;
    // public static final int MAXSIZE = 20000; // Max size of file
    private static final Logger logger       = Logger.getLogger(WebCrawler.class);

    public static void main(String[] argv)
    {

        final WebCrawler wc = new WebCrawler();

        String crawledURLs = null;
        String updateAt = null;

        final Organization org = new Organization();
        org.setName("CityofColumbiaSC");
        org.setState("SC");

        final String folderPath = FolderManager.getFolderPath(org);

        final DateFormat dateFormat = new SimpleDateFormat(
                "yyyy_MM_dd_hh_mm_ss");
        final Calendar cal = Calendar.getInstance();
        updateAt = dateFormat.format(cal.getTime());

        crawledURLs = "WC_baseline_ID_" + "99_" + updateAt;
        new File(folderPath + "/baseline").mkdir();
        BufferedWriter urlWriter = null;
        try
        {
            urlWriter = new BufferedWriter(new FileWriter(folderPath
                    + "/baseline/" + crawledURLs));
        }
        catch (final IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        wc.run("http://columbiasc.net/city-council/agendas", urlWriter, "");
    }

    // URLs to be searched
    // Vector newURLs;
    // Known URLs
    Hashtable                     knownURLs;

    // urls that must be substituted since the actual urls
    // can not be applied such as ones using javascript
    Hashtable<String, String>     substituteAgendaUrls;
    // max number of pages to download
    int                           maxPages;
    String                        urlFormat;

    // used to write
    BufferedWriter                urlWriter;

    // initializes data structures. argv is the command line arguments.

    int                           crawlLevel = 0;

    // Check that the robot exclusion protocol does not disallow
    // downloading url.

    private static final String[] monthNames = { "JANUARY", "FEBRURARY",
            "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER",
            "OCTOBER", "NOVEMBER", "DECEMBER" };

    // adds new URL to the queue. Accept only new URL's that end in
    // htm or html. oldURL is the context, newURLString is the link
    // (either an absolute or a relative URL).

    public void addnewurl(URL oldURL, String newUrlString)

    {
        URL url;
        try
        {

            url = new URL(oldURL, newUrlString);
            // int pos1 = filename.lastIndexOf(".pdf");
            // if(pos1 != -1 && pos1 == filename.length() -4)
            // {
            if ((newUrlString != null) && !newUrlString.equals(""))
            {
                final String fullUrlString = buildUrlOutputString(newUrlString,
                        url);
                if (fullUrlString != null)
                {
                    // System.out.print("addNewUrl() URL String=" +
                    // fullUrlString);
                    if (!knownURLs.containsKey(fullUrlString))
                    {

                        // System.out.println("addNewUrl() URL String=" +
                        // fullUrlString + " format=" + urlFormat);
                        if (agendaFileFormatMatches(urlFormat, fullUrlString))
                        {
                            logger.info("Match Found: "
                                    + fullUrlString.substring(0,
                                            fullUrlString.lastIndexOf("\n")));
                            try
                            {

                                urlWriter.write(fullUrlString);

                            }
                            catch (final IOException ioe)
                            {
                                logger.fatal(ioe.getMessage(), ioe);
                            }
                            knownURLs.put(fullUrlString, new Integer(1));
                        }
                        else
                        {
                            // nt iSuffix = filename.lastIndexOf("htm");
                            // if ((iSuffix == filename.length() - 3) ||
                            // (iSuffix == filename.length() - 4)) {
                            knownURLs.put(fullUrlString, new Integer(1));
                            // newURLs.addElement(url);
                            if (crawlLevel < CRAWL_LIMIT)
                            {
                                if (robotSafe(url))
                                {
                                    final String page = getpage(url);
                                    // if (DEBUG)
                                    // {
                                    // System.out.println(page);
                                    // }
                                    if (page.length() != 0)
                                    {
                                        processpage(url, page);
                                    }

                                    // System.out.println("Found new URL " +
                                    // url.toString());

                                }
                            }
                        }
                    }
                }
            }
            // }
        }
        catch (final MalformedURLException e)
        {
            return;
        }
    }

    private boolean agendaFileFormatMatches(String format, String url)
    {
        boolean isMatch = false;

        final Pattern pat = Pattern.compile(format);
        final Matcher matcher = pat.matcher(url);
        if (matcher.find())
        {
            isMatch = true;
        }

        return isMatch;
    }

    // Download contents of URL

    private String buildUrlOutputString(String newUrlString, URL url)
    {
        String fullUrlString = "";
        final String protocol = url.getProtocol();

        // logger.info("%%%%%1"+newUrlString);
        // logger.info("%%%%%2 url="+url);
        if (!protocol.equals("http") && !protocol.equals("https"))
        {
            fullUrlString = null;
        }
        else
        {
            if (newUrlString != null)
            {

                if ((newUrlString.length() > 3)
                        && !newUrlString.trim().substring(0, 4).equals("http"))
                {

                    final String host = url.getHost();
                    // logger.info(newjjjjjjjjjjjjjjjjjjjjjjjjjjjjUrlString.trim().substring(0,
                    // 3));
                    // if(newUrlString.trim().substring(0, 3).equals("../")){
                    if ((newUrlString.charAt(0) != '/')
                            && (newUrlString.charAt(0) != '.'))
                    {
                        final String urlStr = url.toString();
                        final int pos = urlStr.lastIndexOf("/");
                        final String urlBase = urlStr.substring(0, pos);
                        fullUrlString = urlBase
                                + "/"
                                + newUrlString.substring(newUrlString
                                        .lastIndexOf("/") + 1) + "\n";// substring
                                                                      // of
                                                                      // newUrlString
                                                                      // to
                                                                      // accomodate
                                                                      // links
                                                                      // that do
                                                                      // not
                                                                      // begin
                                                                      // with a
                                                                      // /
                    }
                    else if (newUrlString.contains("../"))
                    {

                        // fullUrlString = protocol+"://"+host+"/"+newUrlString;
                        fullUrlString = url.toString();
                    }
                    else if (newUrlString.contains("./"))
                    {

                        fullUrlString = protocol + "://"
                                + newUrlString.replace("./", host + "/") + "\n";

                    }
                    else
                    {

                        // fullUrlString =
                        // protocol+"://"+host+"//"+newUrlString+"\n";
                        fullUrlString = protocol + "://" + host + newUrlString
                                + "\n";
                    }
                }
                else
                {

                    fullUrlString = newUrlString + "\n";
                }
            }
        }
        try
        {
            fullUrlString = fullUrlString.replace("/./", "/");
            fullUrlString = fullUrlString.replace(" ", "%20");
        }
        catch (final NullPointerException e)
        {
            logger.error("NullPointerException");
            logger.error("WebCrawler.buildUrlOutputString newUrlString = "
                    + newUrlString);
            e.printStackTrace();
        }

        // logger.info("%%%%%%3"+fullUrlString);
        return fullUrlString;
    }

    // Go through page finding links to URLs. A link is signalled
    // by <a href=" ... It ends with a close angle bracket, preceded
    // by a close quote, possibly preceded by a hatch mark (marking a
    // fragment, an internal page marker)

    private String createSubstituteAgendaUrl(String organizationUrlStr,
            String urlPattern)
    {
        String substituteUrl = null;
        final Calendar today = Calendar.getInstance();
        // totday.set(2014, Calendar.MARCH, 20);

        boolean connectionSucceeded = false;

        for (int i = 0; i < 7; i++)
        {
            final int monthInt = today.get(Calendar.MONTH);
            final String monthName = monthNames[monthInt];
            String dateStr = new SimpleDateFormat("MM-dd-yyyy").format(today
                    .getTime());

            // must remove possible leading zero on month and day
            if (dateStr.charAt(3) == '0')
            {
                dateStr = dateStr.substring(0, 2) + dateStr.substring(4);
            }
            if (dateStr.charAt(0) == '0')
            {
                dateStr = dateStr.substring(1);
            }
            substituteUrl = urlPattern.replace("MONTH", monthName);
            substituteUrl = substituteUrl.replace("DATE", dateStr);

            try
            {
                final URL url = new URL(substituteUrl);
                final String page = getpage(url);
                if (!page.contains("Error getting file listing"))
                {
                    connectionSucceeded = true;
                }
            }
            catch (final MalformedURLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                connectionSucceeded = false;
            }
            if (connectionSucceeded)
            {
                break;
            }

            today.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (!connectionSucceeded)
        {
            substituteUrl = null;
        }

        return substituteUrl;

    }

    public String getpage(URL url)
    {
    	int inputLength = 0;
    	
        try
        {
            // try opening the URL
            final URLConnection urlConnection = url.openConnection();
            urlConnection.setReadTimeout(15000);
            // System.out.println("Downloading " + url.toString());

            urlConnection.setAllowUserInteraction(false);

            final InputStream urlStream = url.openStream();
            // search the input stream for links
            // first, read in the entire URL
			inputLength = urlStream.available();

            final byte b[] = new byte[inputLength];
            int numRead = urlStream.read(b);
            String content = new String(b);
            // while ((numRead != -1) && (content.length() < MAXSIZE)) {
            while ((numRead != -1))
            {
                numRead = urlStream.read(b);
                if (numRead != -1)
                {
                    final String newContent = new String(b, 0, numRead);
                    content += newContent;
                }
            }

            return content;

        }
        catch (final IOException e)
        {
            logger.info("ERROR: couldn't open URL ");
            return "";
        }
    }

    public void initialize(String argv, String agendaFileFormat)
    {
        URL url;
        knownURLs = new Hashtable();

        // THe urls that must be substituted are known and must be hard coded
        // into the system.
        substituteAgendaUrls = new Hashtable<String, String>();
        final String key = "www.greenvillesc.gov";
        final String value = "http://www.greenvillesc.gov/CouncilAgendas/FileList.aspx?y=2014&m=MONTH&t=Formal&d=DATE";
        substituteAgendaUrls.put(key, value);

        // newURLs = new Vector();
        urlFormat = agendaFileFormat;

        crawlLevel = 0;

        try
        {
            url = new URL(argv);
        }
        catch (final MalformedURLException e)
        {
            System.out.println("Invalid starting URL " + argv);
            return;
        }
        knownURLs.put(url, new Integer(1));
        // newURLs.addElement(url);
        // System.out.println("Starting search: Initial URL " + url.toString());
        maxPages = SEARCH_LIMIT;
        // if (argv.length > 1) {
        // int iPages = Integer.parseInt(argv[1]);
        // if (iPages < maxPages) maxPages = iPages; }
        // System.out.println("Maximum number of pages:" + maxPages);

        /*
         * Behind a firewall set your proxy and port here!
         */
        final Properties props = new Properties(System.getProperties());
        props.put("http.proxySet", "true");
        props.put("http.proxyHost", "webcache-cup");
        props.put("http.proxyPort", "8080");

        final Properties newprops = new Properties(props);
        System.setProperties(newprops);
        /**/
    }

    public void processpage(URL url, String page)
    {
        int linkCount = 0;
        crawlLevel += 1;
        logger.info("[PROCESS PAGE] url=" + url + " crawlLevel=" + crawlLevel
                + "start");

        try
        {

            final String lcPage = page.toLowerCase(); // Page in lower case
            int index = 0; // position in page
            int iEndAngle, ihref, iURL, iCloseQuote, iHatchMark, iEnd;

            while ((index = lcPage.indexOf("<a", index)) != -1)
            {
                iEndAngle = lcPage.indexOf(">", index);
                ihref = lcPage.indexOf("href", index);
                if (ihref != -1)
                {
                    iURL = lcPage.indexOf("\"", ihref) + 1;
                    if ((iURL != -1) && (iEndAngle != -1) && (iURL < iEndAngle))
                    {
                        iCloseQuote = lcPage.indexOf("\"", iURL);
                        iHatchMark = lcPage.indexOf("#", iURL);
                        if ((iCloseQuote != -1) && (iCloseQuote < iEndAngle))
                        {
                            iEnd = iCloseQuote;
                            if ((iHatchMark != -1)
                                    && (iHatchMark < iCloseQuote))
                            {
                                iEnd = iHatchMark;
                            }
                            final String newUrlString = page.substring(iURL,
                                    iEnd);

                            addnewurl(url, newUrlString);

                            linkCount += 1;
                            // if(linkCount >= linkLimit) // used for testing
                            // {
                            // break;
                            // }

                        }
                    }
                }
                index = iEndAngle;
            }
        }
        catch (final Exception e)
        {
            logger.fatal(e.getMessage(), e);
        }

        logger.info("Total Number of Urls on page = " + linkCount);

        logger.info("[PROCESS PAGE] url=" + url + " crawlLevel=" + crawlLevel
                + " end end end end end end end");
        crawlLevel -= 1;
    }

    // Top-level procedure. Keep popping a url off newURLs, download
    // it, and accumulate new URLs

    public boolean robotSafe(URL url)
    {
        final String strHost = url.getHost();

        // form URL of the robots.txt file
        final String strRobot = "http://" + strHost + "/robots.txt";
        URL urlRobot;
        try
        {
            urlRobot = new URL(strRobot);
        }
        catch (final MalformedURLException e)
        {
            // something weird is happening, so don't trust it
            return false;
        }

        if (DEBUG)
        {
            System.out
                    .println("Checking robot protocol " + urlRobot.toString());
        }
        String strCommands;
        try
        {
            final InputStream urlRobotStream = urlRobot.openStream();

            // read in entire file
            final byte b[] = new byte[1000];
            int numRead = urlRobotStream.read(b);
            if (numRead == -1)
            {
                return true; // appears to be noting in robot file
            }
            strCommands = new String(b, 0, numRead);
            while (numRead != -1)
            {
                numRead = urlRobotStream.read(b);
                if (numRead != -1)
                {
                    final String newCommands = new String(b, 0, numRead);
                    strCommands += newCommands;
                }
            }
            urlRobotStream.close();
        }
        catch (final IOException e)
        {
            // if there is no robots.txt file, it is OK to search
            return true;
        }
        if (DEBUG)
        {
            System.out.println(strCommands);
        }

        // assume that this robots.txt refers to us and
        // search for "Disallow:" commands.
        final String strURL = url.getFile();
        int index = 0;
        while ((index = strCommands.indexOf(DISALLOW, index)) != -1)
        {
            index += DISALLOW.length();
            final String strPath = strCommands.substring(index);
            final StringTokenizer st = new StringTokenizer(strPath);

            if (!st.hasMoreTokens())
            {
                break;
            }

            final String strBadPath = st.nextToken();

            // if the URL starts with a disallowed path, it is not safe
            if (strURL.indexOf(strBadPath) == 0)
            {
                return false;
            }
        }

        return true;
    }

    public void run(String baseUrlString, BufferedWriter urlWriter,
            String agendaFileFormat)

    {
        logger.info("agendaFileFormat=" + agendaFileFormat);

        String basePageUrlStr = baseUrlString;

        this.urlWriter = urlWriter;

        initialize(basePageUrlStr, agendaFileFormat);
        // for (int i = 0; i < maxPages; i++) {
        // if(newURLs.size() > 0)
        // {
        // URL url = (URL) newURLs.elementAt(0);

        // check to see if we need to use a substitute url for this organization
        URL u = null;
        try
        {
            u = new URL(baseUrlString);
        }
        catch (final MalformedURLException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        final String urlPattern = substituteAgendaUrls.get(u.getHost());
        String substituteUrl = null;
        if (urlPattern != null)
        {
            substituteUrl = createSubstituteAgendaUrl(u.getHost(), urlPattern);
            if (substituteUrl != null)
            {
                basePageUrlStr = substituteUrl;
            }
        }

        URL url = null;
        try
        {
            url = new URL(basePageUrlStr);
        }
        catch (final MalformedURLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // newURLs.removeElementAt(0);

        if (DEBUG)
        {
            System.out.println("Searching " + url.toString());
        }
        if (robotSafe(url))
        {
            final String page = getpage(url);
            if (DEBUG)
            {
                System.out.println(page);
            }
            if (page.length() != 0)
            {
                processpage(url, page);
            }
            // if (newURLs.isEmpty()) break;
        }
        // }
        // }

        logger.info("Page Search complete.");
    }

}