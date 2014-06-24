package voterheads;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

public class OrgDownload
{

//    private static final String organizationJSONLink = "https://www.voterheads.com/organizations.json";
//    private static final String organizationJSONLink = "http://127.0.0.1:9990";
    private static final String organizationJSONLink = Voterheads.props.getProperty("organizationJSONLink");
    private static final Logger logger               = Logger.getLogger(OrgDownload.class);

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // TODO Auto-generated method stub
        final OrgDownload download = new OrgDownload();
        download.download();
    }

    public ArrayList<Organization> download()
    {
        URL voterheads;
        ArrayList<Organization> orgs = null;
        // initialize to true, if an exception is thrown set to false
        boolean downloadSuccessful = true;

        String s = null;

        // OrgWithKeyWords[] orgWithKeyWords = null;

        try
        {
            voterheads = new URL(organizationJSONLink);
            final URLConnection vh = voterheads.openConnection();
            vh.setReadTimeout(30000);
            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    vh.getInputStream()));
            String inputLine;
            inputLine = in.readLine();
            in.close();

            inputLine = inputLine.substring(1, inputLine.length() - 1);
            final Gson gson = new Gson();

            int end = 0;
            int start = 0;
            orgs = new ArrayList<Organization>();

            while ((start = inputLine.indexOf("{", end)) != -1)
            {
                end = inputLine.indexOf("},", start);
                s = inputLine.substring(start, end + 1);
                final Organization o = gson.fromJson(s, Organization.class);
                start = inputLine.indexOf("[", end) + 1;
                end = inputLine.indexOf("]", start);
                final String keyWordList = inputLine.substring(start, end);
                // String keyWords = keyWordList.replace("\",\""," ");
                String keyWords = keyWordList.replace(",", " ");
                if (keyWords.length() == 0)
                {
                    keyWords = "";
                }
                o.setKeyWords(keyWords);
                orgs.add(o);
                logger.info("KeyWords for " + o.getName() + ": " + keyWords);
            }

            //
            // orgs = (Organization[]) gson.fromJson(inputLine,
            // Organization[].class);
            // orgWithKeyWords = (OrgWithKeyWords[]) gson.fromJson(inputLine,
            // OrgWithKeyWords[].class);
            //
            // int i= 0;
            // for(OrgWithKeyWords o: orgWithKeyWords)
            // {
            // // System.out.println(org.toString());
            // orgs[i++] = o.getOrganization();
            // }

        }
        catch (final MalformedURLException me)
        {
        	logger.fatal("MalformedURLException organizationJSONLink="+organizationJSONLink , me);
        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            logger.fatal(e, e);
            downloadSuccessful = false;
        }
        finally
        {
            if (downloadSuccessful)
            {
                logger.info("JSON Downloaded Successfully");
            }
            else
            {
                logger.fatal("JSON Failed to download");
            }
        }

        return orgs;
    }

}
