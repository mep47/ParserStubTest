package voterheads;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import org.apache.log4j.Logger;

public class FolderManager
{

    private static Logger logger = null;

    /**
     * @param args
     */
    /**
     * getFolderPath(String OrgazinationNanme){ create path
     * /VoterHeadsFolders/+org.getState()+"/"+yearStr+"-"+org.getId() test if
     * the path does not exist make one return (String) path
     * 
     * @param args
     */
    public static String getFolderPath(Organization org)
    {

        logger = Logger.getLogger(FolderManager.class);

        final BufferedWriter coreWriter = null;

        final String home = System.getProperty("user.home");
        final int yearInt = Calendar.getInstance().get(Calendar.YEAR);
        final String yearStr = Integer.toString(yearInt);
        org.getName().replaceAll("[^a-zA-Z0-9]+", "");
        // space
        // String dir
        // =home+"/solr/voterHeads/solr/voterHeadsFolders/"+org.getState()+"/"+yearStr+alphaAndDigitsOrgName;
        final String dir = home + "/voterHeadsFolders/" + org.getState() + "/"
                + yearStr + "-" + org.getId();
        logger.info("FolderManager.getFolderPath path = " + dir);

        try
        {
            if (!(new File(dir + "/baseline").exists()))
            { // if the folder does not exist, then create one

                new File(dir + "/baseline").mkdirs();
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
                if (coreWriter != null)
                {
                    coreWriter.close();
                }
            }
            catch (final IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                logger.fatal(e.getMessage(), e);
            }
        }

        return dir;
    }
}