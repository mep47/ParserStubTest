package voterheads;

public class FilenameUrlPair
{

    String filename;
    String url;

    public FilenameUrlPair(String filename, String url)
    {
        this.filename = filename;
        this.url = url;
    }

    public String getFilename()
    {
        return filename;
    }

    public String getUrl()
    {
        return url;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

}
