package voterheads;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class RunNutch
{

    private static Logger logger = null;

    public static void createSeedFile(Organization org, String folderPath)
    {
        logger = Logger.getLogger(Voterheads.class);

        BufferedWriter writer = null;
        try
        {
            // String home = System.getProperty("user.home");
            if (!(new File(folderPath + "/urls").exists()))
            { // if the folder does not exist, then create one

                new File(folderPath + "/urls").mkdirs();
            }
            writer = new BufferedWriter(new FileWriter(folderPath
                    + "/urls/seed.txt"));
            final String url = org.getAgenda_url();

            if (url != null)
            {
                writer.write(url);
                logger.info("Writing Seed File Url: " + url);
            }
            else
            {
                logger.info("Seed File Url = null");
            }
        }
        catch (final IOException e)
        {
            // TODO Auto-generated catch block
            logger.fatal(e.getMessage(), e);
        }
        finally
        {
            try
            {
                writer.close();
            }
            catch (final IOException e)
            {
                // TODO Auto-generated catch block
                logger.fatal(e.getMessage(), e);
            }
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // TODO Auto-generated method stub
        new RunNutch().runNutch();
    }

    public static void runSaveNutchUrls()
    {
        try
        {
            final String home = System.getProperty("user.home");

            final Runtime r = Runtime.getRuntime();
            final Process p = r.exec(home
                    + "/apache-nutch-1.7/saveNutchUrls.sh");
            p.waitFor();
            final BufferedReader b = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            String line = "";
            while ((line = b.readLine()) != null)
            {
                logger.info(line);
            }
        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            logger.fatal(e.getMessage(), e);
        }

    }

    public static void writeRunNutchShellScript(String folderPath)
    {
        // write shell script runNutch
        logger = Logger.getLogger(RunNutch.class);

        final String crawlDepth = "1";
        // String crawlTopN = "3";

        logger.info("writeRunNutchShellScript: crawlDepth=" + crawlDepth);

        final String home = System.getProperty("user.home");
        BufferedWriter writeRunNutch = null;
        try
        {
            if (!(new File(folderPath + "/crawl").exists()))
            { // if the folder does not exist, then create one
              // solution 1:
                new File(folderPath + "/crawl").mkdirs();
            }
            writeRunNutch = new BufferedWriter(new FileWriter(home
                    + "/apache-nutch-1.7/runNutch.sh"));
            final String dir = "\n echo \"<<runNutch.sh>>Starting runNutch.sh\""
                    + " \n" + "cd " + home + "/apache-nutch-1.7/ \n";
            // String javaHome =
            // "JAVA_HOME = /usr/lib/jvm/java-7-openjdk-amd64/ \n";
            // String javaHome = "JAVA_HOME = /usr/lib/jvm/java-7-oracle/ \n";
            final String javaHome = "";
            final String main = "bin/nutch crawl " + folderPath + "/urls -dir "
                    + folderPath + "/crawl -depth " + crawlDepth
                    + " > createRunNutchlog.txt";
            writeRunNutch.write("#!/bin/bash \n" + dir + javaHome + main
                    + "\n echo \"<<runNutch.sh>> runNutch.sh Finished\"");
            writeRunNutch.close();
            Runtime.getRuntime().exec(
                    "chmod 777 " + home + "/apache-nutch-1.7/runNutch.sh");
        }
        catch (final IOException e)
        {
            // TODO Auto-generated catch block
            logger.fatal(e.getMessage(), e);
        }
    }

    public static void writeSaveNutchUrlsShellScript(String folderPath)
    {
        final String home = System.getProperty("user.home");
        BufferedReader reader = null;
        String input = null;
        String segment = null;
        try
        {
            reader = new BufferedReader(new FileReader(home
                    + "/apache-nutch-1.7/createRunNutchlog.txt"));
            while ((input = reader.readLine()) != null)
            {

                if (input.contains("Fetcher: segment: "))
                {
                    final int index = input.lastIndexOf("/");
                    segment = input.substring(index + 1);
                    // System.out.println("segment="+segment+"index= "+index);
                    break;
                }

            }
            reader.close();
            // write shell script
            final BufferedWriter writer = new BufferedWriter(new FileWriter(
                    home + "/apache-nutch-1.7/saveNutchUrls.sh"));
            final String dir = "cd " + home + "/apache-nutch-1.7/ \n";
            final String part1 = "bin/nutch readseg -dump " + folderPath
                    + "/crawl/segments/";
            final String part2 = "/ outputdir2 -nocontent -nofetch -nogenerate -noparse -noparsetextless outputdir2/dump > saveNutchUrls_log.txt";
            writer.write("#!/bin/bash \n" + dir
                    + " echo \"<<saveNutchUrls.sh>> start save the urls\"\n"
                    + part1 + segment + part2
                    + "\n echo \"<<saveNutchUrls.sh>>finish save the urls\"");
            writer.close();

            Runtime.getRuntime().exec(
                    "chmod 777 " + home + "/apache-nutch-1.7/saveNutchUrls.sh");
            Thread.sleep(2000);
        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void runNutch()
    {
        try
        {
            final String home = System.getProperty("user.home");

            final Runtime r = Runtime.getRuntime();
            final Process p = r.exec(home + "/apache-nutch-1.7/runNutch.sh");

            p.waitFor();

            final BufferedReader b = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            String line = "";
            while ((line = b.readLine()) != null)
            {
                logger.info(line);
            }

        }
        catch (final Exception e)
        {
            // TODO Auto-generated catch block
            logger.fatal(e.getMessage(), e);
        }

    }

}
