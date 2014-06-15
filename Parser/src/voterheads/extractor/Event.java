package voterheads.extractor;

import voterheads.Voterheads;

import java.util.ArrayList;
import java.util.List;

public class Event
{

    String      orgId;
    String      desription;
    String      docLink;
    String      startTime;
    String      title;
    List<Topic> topics;

    // Constructor
    public Event()
    {
        topics = new ArrayList<Topic>();
    }

    public String getDesription()
    {
        return desription;
    }

    public String getDocLink()
    {
        return docLink;
    }

    public String getOrgId()
    {
        return orgId;
    }

    public String getStartTime()
    {
        return startTime;
    }

    public String getTitle()
    {
        return title;
    }

    public List<Topic> getTopics()
    {
        return topics;
    }

    public void setDesription(String desription)
    {
        this.desription = desription;
    }

    public void setDocLink(String docLink)
    {
        this.docLink = docLink;
    }

    public void setOrgId(String orgId)
    {
        this.orgId = orgId;
    }

    public void setStartTime(String startTime)
    {
        this.startTime = startTime;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setTopics(List<Topic> topics)
    {
        this.topics = topics;
    }

    @Override
    public String toString()
    {
        final StringBuffer str = new StringBuffer(200);

        str.append("orgId: " + orgId + "\n");
        str.append("docLink: " + docLink + "\n\n");
        for (final Topic top : topics)
        {
            str.append("\n" + top);
        }

        return str.toString();

    }

}
