package voterheads;

import java.util.Calendar;
import java.util.Random;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ext.DatabaseFileLockedException;
//import javax.ejb.Stateless;
//import org.jboss.logging.Logger;

public class ObjectManager
{

    // private final static Logger LOGGER =
    // Logger.getLogger(ObjectManager.class.getName());

    private static ObjectContainer db = null;

    static public void closeDb(ObjectContainer db)
    {
        // System.out.println("ObjectManager.closeDb");
        db.close();
        db = null;
    }

    // static public ObjectContainer openDb() throws InterruptedException
    static public ObjectContainer openDb(String dbFilePath, Organization org)
            throws InterruptedException
    {
        // System.out.println("ObjectManager.openDb db="+db);

        final int tryCount = 0;
        boolean locked = false;

        System.getProperty("user.home");
        do
        {
            locked = false;
            try
            {
                // db =
                // Db4oEmbedded.openFile(home+"/voterheads/voterheads.db4o");
                // db =
                // Db4oEmbedded.openFile(dbFilePath+"/"+org.getName().replaceAll("[^a-zA-Z0-9]+","")+".db4o");
                final int yearInt = Calendar.getInstance().get(Calendar.YEAR);
                final String yearStr = Integer.toString(yearInt);
                db = Db4oEmbedded.openFile(dbFilePath + "/" + yearStr + "-"
                        + org.getId() + ".db4o");
            }
            catch (final DatabaseFileLockedException e)
            {
                final Random rand = new Random();

                locked = true;
                final int waitTime = rand.nextInt(250) + 250;
                /*
                 * if(LOGGER.isInfoEnabled()) {
                 * LOGGER.info("Database Locked - wait: "+waitTime); }
                 */
                Thread.sleep(waitTime);
            }
        }
        while (locked && (tryCount < 100));

        return db;
    }

}
