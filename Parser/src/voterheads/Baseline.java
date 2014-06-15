package voterheads;

public class Baseline
{

    /**
     * @param args
     */
    private String name;
    private String id;
    private String hashcode;
    private String update_at;
    private String description;
    private String nutchCrawledURLs;
    private long   sequenceNumber;
    // added by jiting
    private int    weekDiff;
    private int    monthDiff;
    private long   countDays;

    public long getCountDays()
    {
        return countDays;
    }

    public String getDescription()
    {
        return description;
    }

    public String getHashcode()
    {
        return hashcode;
    }

    public String getId()
    {
        return id;
    }

    public int getMonthDiff()
    {
        return monthDiff;
    }

    public String getName()
    {
        return name;
    }

    public String getNutchCrawledURLs()
    {
        return nutchCrawledURLs;
    }

    public long getSequenceNumber()
    {
        return sequenceNumber;
    }

    public String getUpdate_at()
    {
        return update_at;
    }

    public int getWeekDiff()
    {
        return weekDiff;
    }

    public void setCountDays(long countDays)
    {
        this.countDays = countDays;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setHashcode(String hashcode)
    {
        this.hashcode = hashcode;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setMonthDiff(int monthDiff)
    {
        this.monthDiff = monthDiff;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setNutchCrawledURLs(String nutchCrawledURLs)
    {
        this.nutchCrawledURLs = nutchCrawledURLs;
    }

    public void setSequenceNumber(long sequenceNumber)
    {
        this.sequenceNumber = sequenceNumber;
    }

    public void setUpdate_at(String update_at)
    {
        this.update_at = update_at;
    }

    public void setWeekDiff(int weekDiff)
    {
        this.weekDiff = weekDiff;
    }

}
