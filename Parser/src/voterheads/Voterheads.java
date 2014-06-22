package voterheads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import voterheads.extractor.Event;
import voterheads.extractor.Extractor;
import voterheads.extractor.Topic;
import voterheads.index.Indexer;
import voterheads.index.QueryResult;
import voterheads.mail.MailManager;
import voterheads.webcrawler.WebCrawler;
import biz.source_code.base64Coder.Base64Coder;

import com.cribbstechnologies.clients.mandrill.exception.RequestFailedException;
import com.cribbstechnologies.clients.mandrill.model.MandrillAttachment;
import com.cribbstechnologies.clients.mandrill.model.MandrillHtmlMessage;
import com.cribbstechnologies.clients.mandrill.model.MandrillMessageRequest;
import com.cribbstechnologies.clients.mandrill.model.MandrillRecipient;
import com.cribbstechnologies.clients.mandrill.request.MandrillMessagesRequest;
import com.cribbstechnologies.clients.mandrill.request.MandrillRESTRequest;
import com.cribbstechnologies.clients.mandrill.util.MandrillConfiguration;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;

@SuppressWarnings("deprecation")
public class Voterheads
{

    private static MandrillRESTRequest     request         = new MandrillRESTRequest();
    private static MandrillConfiguration   config          = new MandrillConfiguration();
    private static MandrillMessagesRequest messagesRequest = new MandrillMessagesRequest();
    private static HttpClient              client;
    private static ObjectMapper            mapper          = new ObjectMapper();
    public  static Properties              props           = new Properties();
    private static Logger                  logger          = Logger.getLogger(Voterheads.class);
    private static boolean                 isLiveStatus;
    private static boolean                 isReportOnlyStatus;

    public static String calculateHashcode(String folderPath,
            String nutchCrawledURLs)
    {
        System.getProperty("user.home");
        BufferedWriter writerHashcodeShell;
        String hashcode = "";
        try
        {
            writerHashcodeShell = new BufferedWriter(new FileWriter(folderPath
                    + "/baseline/getHashcode.sh"));

            final String dirHashcode = " \n" + "cd " + folderPath
                    + "/baseline\n";
            final String partHashcode = "md5sum ";

            writerHashcodeShell.write("#!/bin/bash \n" + dirHashcode
                    + partHashcode + nutchCrawledURLs);
            writerHashcodeShell.close();
            Runtime.getRuntime().exec(
                    "chmod 777 " + folderPath + "/baseline/getHashcode.sh");
            Thread.sleep(1000);
            // run the hashcode shell script
            final Runtime rHash = Runtime.getRuntime();
            final Process pHash = rHash.exec(folderPath
                    + "/baseline/getHashcode.sh");
            pHash.waitFor();
            final BufferedReader bHash = new BufferedReader(
                    new InputStreamReader(pHash.getInputStream()));

            hashcode = bHash.readLine();
            logger.info("HH: " + hashcode);
            /*
             * while ((hashcode = bHash.readLine()) != null) {
             * System.out.println("hashcode : " + hashcode) ; }
             */

        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.fatal(e.getMessage(), e);
        }
        return hashcode;
    }

    public static ArrayList<String> checkForMultipleAgendaUrls(
            String agenda_urls)
    {
        final ArrayList<String> urls = new ArrayList<String>();
        final String[] urlsArray = agenda_urls
                .split(",http:|;http:|,https:|;https:");
        for (int i = 1; i < urlsArray.length; i++)
        {
            urlsArray[i] = "http:" + urlsArray[i];
        }
        Collections.addAll(urls, urlsArray);

        return urls;
    }

    private static void checkNumDiff(long lastSequenceNumber,
            String folderPath, ArrayList<String> differentUrls, Organization org)
    {
        // SEND ALERT is not functioning propertly so those lines have been
        // commented
        ObjectContainer db = null;
        try
        {
            db = ObjectManager.openDb(folderPath, org);
            final Baseline queryObject = new Baseline();
            queryObject.setSequenceNumber(lastSequenceNumber);
            Baseline baseline = null;
            final ObjectSet<Baseline> res = db.queryByExample(queryObject);
            baseline = res.get(0);

            final int diffCountToday = differentUrls.size();
            final int dayThreshold = 5;
            final int weekThreshold = 10;
            final int monthThreshold = 30;
            final String url = org.getAgenda_url();

            if (diffCountToday >= dayThreshold)
            {
                final String m = org.getName() + " " + " has more than"
                        + dayThreshold + "new urls today"
                        + "\n Please check it";
                // sendAlert(m, url);
                logger.info(m);
            }

            if ((baseline.getCountDays() % 7) == 0)
            {
                if (baseline.getWeekDiff() >= weekThreshold)
                {
                    final String m = org.getName() + " " + " has "
                            + baseline.getWeekDiff() + "new urls"
                            + " \n it is more than the weekThreshold: "
                            + weekThreshold;
                    // sendAlert(m, url);
                    baseline.setWeekDiff(0);
                    db.store(baseline);
                }

            }
            if ((baseline.getCountDays() % 30) == 0)
            {

                if (baseline.getMonthDiff() == 0)
                {
                    final String m = org.getName()
                            + " has 0 new ulrs for a month";
                    logger.info(m);
                    // sendAlert(m, url);
                    // baseline.setCountDays(0);
                    db.store(baseline);

                }
                else if (baseline.getMonthDiff() >= monthThreshold)
                {
                    final String m = org.getName() + " " + " has "
                            + baseline.getWeekDiff() + "new urls"
                            + " \n it is more than the weekThreshold: "
                            + weekThreshold;
                    logger.info(m);
                    sendAlert(m, url);
                    baseline.setMonthDiff(0);
                    // baseline.setCountDays(0);
                    db.store(baseline);
                }

            }

            if (baseline.getCountDays() == 210)
            {
                baseline.setCountDays(0);
            }

            db.close();
        }
        catch (final InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static ArrayList<String> compareNutchFile(
            String preNutchCrawledURLs, String curNutchCrawledURLs,
            String folderPath)
    {

        // home directory
        BufferedReader readPreUrl = null;
        BufferedReader readCurUrl = null;
        final ArrayList<String> differentUrls = new ArrayList<>();
        try
        {
            readPreUrl = new BufferedReader(new FileReader(folderPath
                    + "/baseline/" + preNutchCrawledURLs));
            readCurUrl = new BufferedReader(new FileReader(folderPath
                    + "/baseline/" + curNutchCrawledURLs));
            String preKey = null;
            final HashMap<String, Integer> preUrlMap = new HashMap<>();

            while ((preKey = readPreUrl.readLine()) != null)
            {
                preUrlMap.put(preKey, 1);
            }
            String curKey = null;

            // int cnt = 0; //debugging
            while ((curKey = readCurUrl.readLine()) != null)
            {
                // if(cnt++ > 2) // debugging
                // {
                // break;
                // }
                if (!preUrlMap.containsKey(curKey))
                {
                    // if(curKey.contains("ag_4_1_2014_ap_r.pdf")) //debug
                    // testing
                    differentUrls.add(curKey);

                    // System.out.println("the different key : " + curKey);
                }
            }

        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            logger.fatal(e.getMessage(), e);
        }
        finally
        {
            try
            {
                readPreUrl.close();
                readCurUrl.close();
            }
            catch (final IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                logger.fatal(e.getMessage(), e);
            }
        }
        return differentUrls;
        // TODO Auto-generated method stub

    }

    private static void doIndexAndParse(List<String> differentUrls,
            String folderPath, Organization org, boolean regexPresent)
    {
        String fileExtension = "";
        FilenameUrlPair pair = null;
        String mailMessage = "";
        QueryResult queryResult = null;
        String statusString = null;

        logger.info("Starting method doIndexAndParse");
        
        Event event = null;
        // Build Message Header for urls with no agenda file format
        mailMessage += "<html><body>";
        mailMessage += "<h1>" + org.getName() + "</h1>";
        mailMessage += "<h2>" + differentUrls.size() + " NEW URLs";

        for (String urlString : differentUrls)
        {
            pair = downloadDiffPage(urlString, folderPath);
            if (pair == null)
            {
                continue;
            }
            if (isLiveStatus)
            {
                statusString = "LIVE";
            }
            if (isReportOnlyStatus)
            {
                statusString = "REPORT_ONLY";
            }
            logger.info("File Extension: "
                    + Indexer.getFileExtension(urlString));
            Indexer.createIndex(pair, folderPath);
            queryResult = Indexer.keyWordQuery(folderPath,
                    org.getKeyWords());
            // urlString.lastIndexOf("/") yeilds a file name that starts with a
            // "/" the + 1 is there to jump to the character after the / as the
            // start of the string
            mailMessage += "<h3><a href=\"" + urlString + "\">"
                    + urlString.substring(urlString.lastIndexOf("/") + 1)
                    + "</a></h3>";
            try
            {
                mailMessage += "<p>" + queryResult.getQueryResult() + "</p>";
            }
            catch (Exception eMailMessageQueryResult)
            {
                logger.warn("Query Result is null");
            }
            if (isReportOnlyStatus)
            {
                if ((org.getAgenda_file_format() != null)
                        && !org.getAgenda_file_format().equals(""))
                {
                    if (props.getProperty("sendJSON").equalsIgnoreCase("True"))
                    {
                        try
                        {
                            Indexer.sendJSON(pair.getUrl(), queryResult,
                                    org.getId(), statusString);
                        }
                        catch (Exception eSendJSON1)
                        {
                            logger.error(eSendJSON1);
                        }
                    }
                }
            }
            else if (isLiveStatus)
            {
                if ((org.getAgenda_file_format() != null)
                        && !org.getAgenda_file_format().equals(""))
                {
                    if (org.getProcess_on_back_end())
                    {
                        Extractor.performExtractionParse(pair, org,
                                queryResult, statusString);
                        // logger.info("doIndexAndParse Line 477 isLivestatus sendJSON: "+props.getProperty("sendJSON"));
                    }
                    else
                    {
                        if (props.getProperty("sendJSON").equalsIgnoreCase(
                                "True"))
                        {
                            try
                            {
                                Indexer.sendJSON(pair.getUrl(), queryResult,
                                        org.getId(), statusString);
                            }
                            catch (Exception eSendJSON2)
                            {
                                logger.error(eSendJSON2);
                            }
                        }
                    }
                }

            }

        }
        mailMessage += "</body></html>";
        if (!regexPresent)
        {
            MailManager.sendEmail(mailMessage,
                    props.getProperty("differenceEmailAddresses"),
                    "info@voterheads.com", org.getName() + " REPORT");
        }

//  Stuff for testing
//        event = Extractor.performExtractionParse(pair, org,
//                queryResult, statusString);
//        
//        if(props.getProperty("production").equals("false") )
//        {
//        	StringBuffer strbuf = new StringBuffer(2000);
//            for (final Topic top : event.getTopics())
//            {
//                strbuf.append(top);
//            }
//
//            final String home = System.getProperty("user.home");
//            String jsonTestResultFilename = home+"/VoterheadsTest/TestOutput/testResult.txt";
//            BufferedWriter out = null;            try
//            {
//            	out = new BufferedWriter(new FileWriter( new File(jsonTestResultFilename)));
//            	out.write(strbuf.toString());
//            }
//            catch(Exception e)
//            {
//            	e.printStackTrace();
//            }
//            finally
//            {
//            	if(out != null)
//            	{
//            		try {
//    					out.close();
//    				} catch (IOException e) {
//    					// TODO Auto-generated catch block
//    					e.printStackTrace();
//    				}
//            	}
//            }
//
//
//        }

    }

    private static FilenameUrlPair downloadDiffPage(String urlString,
            String folderPath)
    {
        URL url = null;
        int pos = folderPath.lastIndexOf("/");

        String outFilename = null;
        byte[] inputLine = new byte[1024];
        int inputLength;

        InputStream in = null;
        FileOutputStream out = null;

        FilenameUrlPair pair = null;
        pos = urlString.lastIndexOf("/");
        String filename = urlString.substring(pos + 1).replace(" ", "");

        try
        {
            url = new URL(urlString.replace(" ", "%20"));
            URLConnection urlConnection = url.openConnection();
            urlConnection.setReadTimeout(15000);
            in = urlConnection.getInputStream();

            DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
            Calendar cal = Calendar.getInstance();
            String fileDate = dateFormat.format(cal.getTime());

            new File(folderPath + "/pages/").mkdir();
            outFilename = folderPath + "/pages/DT_" + fileDate + "_" + filename;

            pair = new FilenameUrlPair(outFilename, url.toString());

            out = new FileOutputStream(outFilename);

            final String fileExtension = filename.substring(
                    filename.lastIndexOf("."), filename.length());
            if (fileExtension.equalsIgnoreCase("htm")
                    || fileExtension.equalsIgnoreCase("html"))
            {
                // prepend doctype
            }

//            while ((inputLength = in.read(inputLine)) != -1)
            while ((inputLength = in.read(inputLine)) != -1)
            {
                out.write(inputLine, 0, inputLength);
            }
            out.flush();
            out.close();
            in.close();

        }
        catch (MalformedURLException mfe)
        {
            logger.error("Malforded URL - " + urlString);
            logger.error(mfe.getMessage(), mfe);
            pair = null;
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage(), ex);
            logger.error(ex.getStackTrace());
            pair = null;
        }

        return pair;
    }

    /*
     * private static List<FilenameUrlPair> downloadDiffPages( List<String>
     * differentUrls, String folderPath) { URL url = null; int pos =
     * folderPath.lastIndexOf("/");
     * 
     * String outFilename = null; final byte[] inputLine = new byte[1024]; int
     * inputLength;
     * 
     * final List<FilenameUrlPair> downloadedPagesFilenames = new
     * ArrayList<FilenameUrlPair>();
     * 
     * InputStream in = null; FileOutputStream out = null;
     * 
     * for (final String urlString : differentUrls) { pos =
     * urlString.lastIndexOf("/"); final String filename =
     * urlString.substring(pos + 1).replace(" ", "");
     * 
     * try { url = new URL(urlString.replace(" ", "%20")); final URLConnection
     * urlConnection = url.openConnection(); in =
     * urlConnection.getInputStream();
     * 
     * final DateFormat dateFormat = new SimpleDateFormat(
     * "yyyy_MM_dd_hh_mm_ss"); final Calendar cal = Calendar.getInstance();
     * final String fileDate = dateFormat.format(cal.getTime());
     * 
     * new File(folderPath + "/pages/").mkdir(); outFilename = folderPath +
     * "/pages/DT_" + fileDate + "_" + filename;
     * 
     * final FilenameUrlPair pair = new FilenameUrlPair(outFilename,
     * url.toString()); downloadedPagesFilenames.add(pair);
     * 
     * out = new FileOutputStream(outFilename); while ((inputLength =
     * in.read(inputLine)) != -1) { out.write(inputLine, 0, inputLength); }
     * out.flush(); out.close(); in.close();
     * 
     * } catch (final MalformedURLException mfe) {
     * logger.error("Malforded URL - " + urlString);
     * logger.error(mfe.getMessage(), mfe); continue; } catch (final Exception
     * ex) { logger.error(ex.getMessage(), ex); continue; }
     * 
     * }
     * 
     * return downloadedPagesFilenames; }
     */

    public static Logger getLogger()
    {
        return logger;
    }

    // end added by jiting

    public static Properties getProperties()
    {
        return props;
    }

    private static Set<String> getPropertiesSet(String field)
    {
        final Set<String> theSet = new HashSet<String>();
        for (String item : props.getProperty(field, "").split(","))
        {
            if (item != null)
            {
                theSet.add(item);
            }
        }
        return theSet;
    }

    private static void iterateAgendaURLs(ArrayList<String> agendaUrls,
            final Organization org, final BufferedWriter urlWriter)
    {
        if (agendaUrls != null)
        {
            for (final String agendaUrl : agendaUrls)
            {
                if (agendaUrl != null)
                {
                    try
                    {

                        final WebCrawler crawler = new WebCrawler();
                        crawler.run(agendaUrl, urlWriter,
                                org.getAgenda_file_format());

                    }
                    catch (final Exception e)
                    {
                        logger.fatal(e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * Steps in main method: 1. Read the property file named .voterheadsrc which
     * must be created in the users home directory and set the appropriate
     * variables. The properties contained in the property file are:
     * differenceEmailAddresses - email address of person/group receiving emails
     * with the difference information. 2. Check to see if there are multiple
     * agenda urls for this org 3. Create the file which will potentially become
     * the new baseline file. The baseline is used to compare todays urls to the
     * previous baseline to see if there have been any changes since yesterday.
     * 4. Loop through the Agenda Urls. 5. Call the webcrawler 6. Create
     * hashcode for todays Urls and compare to previous baseline hashcode 7. If
     * no differences then store new baseline info in database. 8. otherwise
     * read the previous baseline info from the database 9. Create the solr
     * index getting back filename, UrlName pairs for each file/url to process
     * 10. create agenda word index for keyword query 11. Perform keyword query
     * on each agenda page 12. Perform Extraction Parse on each agenda page
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        InputStream input = null;
        final String home = System.getProperty("user.home");
        readPropertiesFile(input, home);

        // Properties file must be read before it can be queried
        final Set<String> runSet = getPropertiesSet("run");
        final Set<String> skipSet = getPropertiesSet("skip");

        processArgs(args, runSet, skipSet);

        logger.info("Run Organizations: " + runSet.toString());
        logger.info("Skip Organizations: " + skipSet.toString());

        final OrgDownload orgDownload = new OrgDownload();
        final ArrayList<Organization> orgs = orgDownload.download();
        ArrayList<String> agendaUrls = null;
        String updateAt = null;

        // file which contains the baseline urls
        String baselineFilename = null;

        // creates a custom logger and log messages

        logger.info("starting voterheads: \n");

        for (final Organization org : orgs)
        {
        	logger.info("org="+org);
            final boolean regexPresent = (org.getAgenda_file_format() != null)
                    && !org.getAgenda_file_format().equals("");
            replaceAgendaDates(org, regexPresent, 0);
            isLiveStatus = org.getStatus_key().equals("LIVE");
            isReportOnlyStatus = org.getStatus_key().equals("REPORT_ONLY");
            /*
             * isLiveStatus = org.getStatus_key().equals("LIVE") &&
             * regexPresent; isReportOnlyStatus =
             * org.getStatus_key().equals("REPORT_ONLY") && regexPresent;
             */

            /*
             * When in testing mode only run orgs in the test set Test set needs
             * to be moved to a testing configuration file
             */

            if (skipSet.contains(org.getName().trim())
                    || skipSet.contains("all"))
            {
                logger.info(org.getName().trim()
                        + " is in the set of organizations to skip :: Skipping");
                continue;
            }
            else if (!runSet.contains(org.getName().trim())
                    && !runSet.contains("all")) // for testing only
            {
                continue;
            }
            /*
             * Skip organization if process on backend is unchecked or skip if
             * it isn't live or report only status (Draft Status)
             */
            if (!isLiveStatus && !isReportOnlyStatus)
            {
                logger.info(org.getName().trim() + " Status is draft::Skipping");
                continue;
            }
            logger.info("********** Starting " + org.getName() + " "
                    + " statusKey=" + org.getStatus_key() + " Regex present: "
                    + (regexPresent ? "YES" : "NO").toString()
                    + " *************");
            /*
             * Only concerned with parsing pdfs at this point if no regex is
             * present then assume we are matching all pdfs process all pdfs
             * when no regex is present for indexer and query
             */
            if (!regexPresent)
            {
                org.setAgendaFileFormat(".\\.pdf");
            }

            logger.info("Agenda_Url = " + org.getAgenda_url());

            final String folderPath = FolderManager.getFolderPath(org);

            try
            {
                if (org.getAgenda_url() != null)
                {
                    agendaUrls = checkForMultipleAgendaUrls(org.getAgenda_url());
                }
                else
                {
                    agendaUrls = null;
                }

                new File(folderPath + "/baseline").mkdir();

                final DateFormat dateFormat = new SimpleDateFormat(
                        "yyyy_MM_dd_hh_mm_ss");
                final Calendar cal = Calendar.getInstance();
                updateAt = dateFormat.format(cal.getTime());

                baselineFilename = "WC_baseline_ID_" + org.getId() + "_"
                        + updateAt;
                final BufferedWriter urlWriter = new BufferedWriter(
                        new FileWriter(folderPath + "/baseline/"
                                + baselineFilename));

                // Loop through agenda Urls
                iterateAgendaURLs(agendaUrls, org, urlWriter);
                urlWriter.close();
            }
            catch (final IOException ioe)
            {
                logger.fatal(ioe.getMessage(), ioe);
            }

            try
            {
                // logger.info("Total Urls count = "+totalUrlsCount);
                // calculate the hashcode
                String hashcode = null;
                ArrayList<String> differentUrls = null;
                ObjectContainer db = null;
                if (baselineFilename != null)
                {
                    hashcode = calculateHashcode(folderPath, baselineFilename);

                    // System.out.println("hashcode = "+hashcode);

                    // push data into dbo4
                    db = ObjectManager.openDb(folderPath, org);
                    Baseline preBaseline = null;

                    final List<BaselineSequence> result = db
                            .query(BaselineSequence.class);
                    BaselineSequence baselineSequence = null;
                    if (result.size() == 0)
                    {

                        baselineSequence = new BaselineSequence();
                        baselineSequence.setLastSequenceNumber(1);
                        db.store(baselineSequence);

                        final Baseline baseline = new Baseline();
                        baseline.setId(org.getId());
                        baseline.setDescription(org.getDescription());
                        baseline.setName(org.getName());
                        baseline.setUpdate_at(updateAt);
                        baseline.setHashcode(hashcode.substring(0,
                                hashcode.indexOf(" ")));
                        baseline.setNutchCrawledURLs(baselineFilename);
                        baseline.setSequenceNumber(baselineSequence
                                .getLastSequenceNumber());
                        // added by jiitng
                        baseline.setCountDays(1);
                        baseline.setWeekDiff(0);
                        baseline.setMonthDiff(0);
                        // end adding
                        db.store(baseline);
                    }
                    else
                    {
                        baselineSequence = result.get(0);
                        final long lastSequenceNumber = baselineSequence
                                .getLastSequenceNumber();
                        final Baseline queryObject = new Baseline();
                        queryObject.setSequenceNumber(lastSequenceNumber);
                        final ObjectSet<Baseline> res = db
                                .queryByExample(queryObject);
                        preBaseline = res.get(0);
                        // added by jiting
                        preBaseline
                                .setCountDays(preBaseline.getCountDays() + 1);
                        db.store(preBaseline);
                        // end adding

                    }

                    differentUrls = new ArrayList<String>();

                    if (preBaseline == null)
                    {
                        BufferedReader readCurrentBaseline = null;
                        readCurrentBaseline = new BufferedReader(
                                new FileReader(folderPath + "/baseline/"
                                        + baselineFilename));

                        String baselineUrl = null;
                        while ((baselineUrl = readCurrentBaseline.readLine()) != null)
                        {
                            differentUrls.add(baselineUrl);
                        }
                        readCurrentBaseline.close();

                        if ((differentUrls != null)
                                && (differentUrls.size() > 0))
                        {
                            logger.info("organization : " + org.getName()
                                    + " has " + differentUrls.size()
                                    + " new Urls");
                            writeDifferenceFile(differentUrls, folderPath);
                            baselineSequence
                                    .setLastSequenceNumber(baselineSequence
                                            .getLastSequenceNumber() + 1);// increase
                                                                          // the
                                                                          // last
                                                                          // sequence
                                                                          // number
                            db.store(baselineSequence);

                            final Baseline baseline = new Baseline();
                            baseline.setId(org.getId());
                            baseline.setDescription(org.getDescription());
                            baseline.setName(org.getName());
                            baseline.setUpdate_at(updateAt);
                            baseline.setHashcode(hashcode.substring(0,
                                    hashcode.indexOf(" ")));
                            baseline.setNutchCrawledURLs(baselineFilename);
                            baseline.setSequenceNumber(baselineSequence
                                    .getLastSequenceNumber());
                            db.store(baseline);

                            doIndexAndParse(differentUrls, folderPath, org,
                                    regexPresent);
                        }
                    }
                    else if (!(hashcode.substring(0, hashcode.indexOf(" "))
                            .equals(preBaseline.getHashcode())))
                    {
                        // System.out.println("pre hashcode : \n" +
                        // preBaseline.getHashcode());
                        // System.out.println("cur hashcode : \n" +
                        // hashcode.substring(0,hashcode.indexOf(" ")));

                        logger.info("preBaseline.getNutchCrawledURLs() = "
                                + preBaseline.getNutchCrawledURLs());
                        logger.info("nutchCrawledURLs = " + baselineFilename);
                        differentUrls = compareNutchFile(
                                preBaseline.getNutchCrawledURLs(),
                                baselineFilename, folderPath);// find the
                                                              // different urls
                        // logger.info("organization : "+org.getName()
                        // +" has "+differentUrls.size()+" new Urls");
                        if ((differentUrls != null)
                                && (differentUrls.size() > 0))
                        {
                            logger.info("organization : " + org.getName()
                                    + " has " + differentUrls.size()
                                    + " new Urls");
                            writeDifferenceFile(differentUrls, folderPath);
                            baselineSequence
                                    .setLastSequenceNumber(baselineSequence
                                            .getLastSequenceNumber() + 1);// increase
                                                                          // the
                                                                          // last
                                                                          // sequence
                                                                          // number
                            db.store(baselineSequence);

                            final Baseline baseline = new Baseline();
                            baseline.setId(org.getId());
                            baseline.setDescription(org.getDescription());
                            baseline.setName(org.getName());
                            baseline.setUpdate_at(updateAt);
                            baseline.setHashcode(hashcode.substring(0,
                                    hashcode.indexOf(" ")));
                            baseline.setNutchCrawledURLs(baselineFilename);
                            baseline.setSequenceNumber(baselineSequence
                                    .getLastSequenceNumber());
                            // added by jiting
                            baseline.setWeekDiff(preBaseline.getWeekDiff()
                                    + differentUrls.size());
                            baseline.setMonthDiff(preBaseline.getMonthDiff()
                                    + differentUrls.size());
                            // end adding
                            db.store(baseline);

                            doIndexAndParse(differentUrls, folderPath, org,
                                    regexPresent);

                        }
                        else
                        {
                            new File(folderPath + "/baseline/"
                                    + baselineFilename).delete();
                        }
                    }
                    else if ((preBaseline != null)
                            && (hashcode.substring(0, hashcode.indexOf(" "))
                                    .equals(preBaseline.getHashcode())))
                    {
                        new File(folderPath + "/baseline/" + baselineFilename)
                                .delete();
                    }

                    /*
                     * 
                     * //start add send alert //added by jiting 11/11/13
                     * 
                     * int count = differentUrls.size(); int dayThreshold = 10;
                     * int week = 7; int weekThreshold = 100; int month = 30;
                     * String url = org.getAgenda_url(); if(count >
                     * dayThreshold){ String m = org.getName() + " " +
                     * " has more than 10 new urls today"; sendAlert(m, url);
                     * logger.info(m); }
                     * 
                     * accuCounter[0]++; //count accumulate days accuCounter[1]
                     * += count; //count accumulate urls
                     * 
                     * if(accuCounter[0] >= week && accuCounter[1] >=
                     * weekThreshold ){ String m = org.getName() + " " +
                     * " has more than 100 new urls in 7 days"; logger.info(m);
                     * sendAlert(m, url); accuCounter[0] = 0; accuCounter[1] =
                     * 0; } if(accuCounter[0] >= month && accuCounter[0] == 0){
                     * String m = org.getName() + " " +
                     * "can not find new update for the past 30 days";
                     * sendAlert(m, url); logger.info(m); accuCounter[0] = 0;
                     * accuCounter[1] = 0; }
                     * 
                     * // end ------------add by jiting
                     */
                    final List<BaselineSequence> newResult = db
                            .query(BaselineSequence.class);
                    BaselineSequence newBaselineSequence = null;
                    newBaselineSequence = newResult.get(0);
                    final long lastSequenceNumber = newBaselineSequence
                            .getLastSequenceNumber();

                    db.close();
                    // start add send alert
                    // added by jiting 11/11/13
                    checkNumDiff(lastSequenceNumber, folderPath, differentUrls,
                            org);
                    // end ------------add by jiting

                } // goes with if(nutchCrawledURLs != null)
                logger.info("======= finish " + org.getName() + " =========\n");

            }
            catch (final Exception e)
            {
                // TODO Auto-generated catch block
                logger.fatal(e.getMessage(), e);
                System.exit(0);
            }
        }
    }

    private static void processArgs(String[] args, final Set<String> runSet,
            final Set<String> skipSet)
    {
        if (args.length >= 2)
        {
            if (args[0].equals("-r"))
            {
                runSet.clear();
                skipSet.clear();
                skipSet.add("none");
                for (String arg : args[1].split(","))
                {
                    runSet.add(arg);
                }
            }
            else if (args[0].equals("-s"))
            {
                runSet.clear();
                runSet.add("all");
                skipSet.clear();
                for (String arg : args[1].split(","))
                {
                    skipSet.add(arg);
                }
            }
        }
    }

    private static void readPropertiesFile(InputStream input, final String home)
    {
        try
        {
            input = new FileInputStream(home + "/.voterheadsrc");

            props.load(input);

            logger.info("PRODUCTION: " + props.getProperty("production"));
            logger.info("SENDJSON: " + props.getProperty("sendJSON"));
            logger.info("SENDJSONLINK: " + props.getProperty("sendJSONLink"));
            logger.info("ORGANIZATIONJASONLINK: " + props.getProperty("organizationJSONLink"));
            logger.info("DIFFERENCEEMAILADDRESSES: "
                    + props.getProperty("differenceEmailAddresses"));

        }
        catch (final Exception e1)
        {
            logger.fatal(e1.getMessage(), e1);
            e1.printStackTrace();
            System.exit(0);
        }
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (final Exception e2)
                {
                    logger.warn(e2.getMessage(), e2);
                    e2.printStackTrace();
                }
            }

        }
    }

    private static void replaceAgendaDates(final Organization org,
            final boolean regexPresent, int offset)
    {
        Calendar calendar = Calendar.getInstance();
        if (offset != 0)
        {
            calendar.add(Calendar.DAY_OF_MONTH, offset);
        }
        if (regexPresent && org.getAgenda_file_format().contains("%YYYY"))
        {
            org.setAgendaFileFormat(org.getAgenda_file_format().replaceAll(
                    "%YYYY", calendar.get(calendar.YEAR) + ""));
        }
        if (regexPresent && org.getAgenda_file_format().contains("%yy"))
        {
            org.setAgendaFileFormat(org.getAgenda_file_format().replaceAll(
                    "%yy", (calendar.get(Calendar.YEAR) % 2000) + ""));
        }
        if (regexPresent && org.getAgenda_file_format().contains("%mm"))
        {
            org.setAgendaFileFormat(org.getAgenda_file_format().replaceAll(
                    "%mm",
                    String.format("%02d", calendar.get(Calendar.MONTH) + 1)));
        }
        if (regexPresent && org.getAgenda_file_format().contains("%dd"))
        {
            org.setAgendaFileFormat(org.getAgenda_file_format().replaceAll(
                    "%dd",
                    String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))));
        }
        if (org.getAgenda_url() != null)
        {
            if (org.getAgenda_url().contains("%YYYY"))
            {
                org.setAgenda_url(org.getAgenda_url().replaceAll("%YYYY",
                        calendar.get(Calendar.YEAR) + ""));
            }
            if (org.getAgenda_url().contains("%yy"))
            {
                org.setAgenda_url(org.getAgenda_url().replaceAll("%yy",
                        (calendar.get(Calendar.YEAR) % 2000) + ""));
            }
            if (org.getAgenda_url().contains("%mm"))
            {
                org.setAgenda_url(org.getAgenda_url()
                        .replaceAll(
                                "%mm",
                                String.format("%02d",
                                        calendar.get(Calendar.MONTH) + 1)));
            }
            if (org.getAgenda_url().contains("%dd"))
            {
                org.setAgenda_url(org.getAgenda_url().replaceAll(
                        "%dd",
                        String.format("%02d",
                                calendar.get(Calendar.DAY_OF_MONTH))));
            }
        }
    }

    // added by jiting
    public static void sendAlert(String m, String url)
    {
        logger.info("**********Sending Alert**********");
        config.setApiKey("K8DAVgcw9DjYrNU2FAqq5g");
        config.setApiVersion("1.0");
        config.setBaseURL("https://mandrillapp.com/api");
        request.setConfig(config);
        request.setObjectMapper(mapper);
        messagesRequest.setRequest(request);

        client = new DefaultHttpClient();
        request.setHttpClient(client);

        final MandrillMessageRequest mmr = new MandrillMessageRequest();
        final MandrillHtmlMessage message = new MandrillHtmlMessage();
        final Map<String, String> headers = new HashMap<String, String>();
        message.setFrom_email("info@voterheads.com");
        message.setFrom_name("VoterHeads");
        message.setHeaders(headers);
        message.setHtml("<html><body><h1>ALERT</h1>" + m
                + "\n URLS: <a href=\"" + url + "\">Org Url</a></body></html>");
        message.setSubject("ALERT!!!!");

        new ArrayList<MandrillAttachment>();
        System.getProperty("user.home");

        // new MandrillRecipient("Michael Price", "mikeprice@voterheads.com"),
        final MandrillRecipient[] recipients = new MandrillRecipient[] { new MandrillRecipient(
                props.getProperty("differenceEmailAddresses"),
                props.getProperty("differenceEmailAddresses")) };
        // System.out.println("name : "+message.getAttachments().get(0).getName());
        // System.out.println("type : "+message.getAttachments().get(0).getType());
        // System.out.println("content : "+message.getAttachments().get(0).getContent());
        message.setTo(recipients);
        message.setTrack_clicks(true);
        message.setTrack_opens(true);

        final String[] tags = new String[1];
        message.setTags(tags);
        mmr.setMessage(message);

        try
        {
            messagesRequest.sendMessage(mmr);
        }
        catch (final RequestFailedException e)
        {
            logger.fatal(e.getMessage(), e);
        }

    }

    public static void sendEmail(String folderPath, String fileName,
            String orgName)
    {
        logger.info("### Sending Email ###");
        config.setApiKey("K8DAVgcw9DjYrNU2FAqq5g");
        config.setApiVersion("1.0");
        config.setBaseURL("https://mandrillapp.com/api");
        request.setConfig(config);
        request.setObjectMapper(mapper);
        messagesRequest.setRequest(request);

        client = new DefaultHttpClient();
        request.setHttpClient(client);

        final MandrillMessageRequest mmr = new MandrillMessageRequest();
        final MandrillHtmlMessage message = new MandrillHtmlMessage();
        final Map<String, String> headers = new HashMap<String, String>();
        message.setFrom_email("info@voterheads.com");
        message.setFrom_name("VoterHeads");
        message.setHeaders(headers);
        message.setHtml("<html><body><h1>Please see attached URL differences file.</h1></body></html>");
        final DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
        final Calendar cal = Calendar.getInstance();
        final String mailDate = dateFormat.format(cal.getTime());
        message.setSubject("URL Diffs on " + mailDate + " for " + orgName);

        final List<MandrillAttachment> attachments = new ArrayList<MandrillAttachment>();
        System.getProperty("user.home");

        // read difference url file
        BufferedReader diffUrls;
        String urls = "";
        char[] diffUrlsArray;
        try
        {
            diffUrls = new BufferedReader(new FileReader(fileName));
            final long fileLength = new File(fileName).length();

            diffUrlsArray = new char[(int) fileLength];

            diffUrls.read(diffUrlsArray, 0, (int) fileLength);
            urls = new String(diffUrlsArray);
        }
        catch (final Exception e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        attachments.add(new MandrillAttachment("text/plain", "test.txt",
                Base64Coder.encodeString(urls)));

        message.setAttachments(attachments);

        final String[] emailAddresses = props.getProperty(
                "differenceEmailAddresses").split(",");

        final MandrillRecipient[] recipients = new MandrillRecipient[emailAddresses.length];
        int idx = 0;
        for (final String str : emailAddresses)
        {
            final MandrillRecipient recip = new MandrillRecipient("", str);
            recipients[idx++] = recip;
        }

        // MandrillRecipient[] recipients = new MandrillRecipient[]{new
        // MandrillRecipient("backendnotifications",
        // "backendnotifications@voterheads.com"), new
        // MandrillRecipient("jiting", "jiting@voterheads.com")};
        // System.out.println("name : "+message.getAttachments().get(0).getName());
        // System.out.println("type : "+message.getAttachments().get(0).getType());
        // System.out.println("content : "+message.getAttachments().get(0).getContent());
        message.setTo(recipients);
        message.setTrack_clicks(true);
        message.setTrack_opens(true);

        final String[] tags = new String[1];
        message.setTags(tags);
        mmr.setMessage(message);

        try
        {
            messagesRequest.sendMessage(mmr);
        }
        catch (final RequestFailedException e)
        {
            e.printStackTrace();
            // fail(e.getMessage());
        }

    }

    private static String writeDifferenceFile(ArrayList<String> differentUrls,
            String folderPath)
    {
        System.getProperty("user.home");
        BufferedWriter writerUrls;
        String fileName = null;

        try
        {
            final DateFormat dateFormat = new SimpleDateFormat(
                    "yyyy_MM_dd_hh_mm_ss");
            final Calendar cal = Calendar.getInstance();
            final String fileDate = dateFormat.format(cal.getTime());

            new File(folderPath + "/diffs/").mkdir();

            fileName = folderPath + "/diffs/diff_" + fileDate + ".txt";

            writerUrls = new BufferedWriter(new FileWriter(fileName));
            for (final String s : differentUrls)
            {
                writerUrls.write(s + "\n");
            }

            writerUrls.close();
        }
        catch (final IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.fatal(e.getMessage(), e);
        }

        return fileName;
    }

    /*
     * // added by jiting public static void sendAlert(String m, String url) {
     * logger.info("**********Sending Alert**********");
     * config.setApiKey("K8DAVgcw9DjYrNU2FAqq5g"); config.setApiVersion("1.0");
     * config.setBaseURL("https://mandrillapp.com/api");
     * request.setConfig(config); request.setObjectMapper(mapper);
     * messagesRequest.setRequest(request);
     * 
     * client = new DefaultHttpClient(); request.setHttpClient(client);
     * 
     * 
     * MandrillMessageRequest mmr = new MandrillMessageRequest();
     * MandrillHtmlMessage message = new MandrillHtmlMessage(); Map<String,
     * String> headers = new HashMap<String, String>();
     * message.setFrom_email("info@voterheads.com");
     * message.setFrom_name("VoterHeads"); message.setHeaders(headers);
     * message.setHtml("<html><body><h1>ALERT</h1>" + m + "<a href=\"" + url
     * +"\">Org Url</a></body></html>"); message.setSubject("ALERT!!!!");
     * 
     * List<MandrillAttachment> attachments = new
     * ArrayList<MandrillAttachment>(); String home =
     * System.getProperty("user.home");
     * 
     * 
     * 
     * MandrillRecipient[] recipients = new MandrillRecipient[]{new
     * MandrillRecipient("Michael Price", "mikeprice@voterheads.com"), new
     * MandrillRecipient("jiting", "jiting@voterheads.com")}; //
     * System.out.println("name : "+message.getAttachments().get(0).getName());
     * //
     * System.out.println("type : "+message.getAttachments().get(0).getType());
     * //
     * System.out.println("content : "+message.getAttachments().get(0).getContent
     * ()); message.setTo(recipients); message.setTrack_clicks(true);
     * message.setTrack_opens(true);
     * 
     * String[] tags = new String[1]; message.setTags(tags);
     * mmr.setMessage(message);
     * 
     * try { SendMessageResponse response = messagesRequest.sendMessage(mmr); //
     * System.out.println("Response="+response.getList().get(0).getStatus()); }
     * catch (RequestFailedException e) { logger.fatal(e.getMessage(), e); }
     * 
     * }
     * 
     * // end added by jiting
     */

}