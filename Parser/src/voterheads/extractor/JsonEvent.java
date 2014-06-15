package voterheads.extractor;

import voterheads.Voterheads;

public class JsonEvent
{

    String description;
    String doc_link;
    String start_time;
    String title;
    String organization_ids;

    public JsonEvent()
    {
        
    }

    public String getDescription()
    {
        return description;
    }

    public String getDoc_link()
    {
        return doc_link;
    }

    public String getOrganization_ids()
    {
        return organization_ids;
    }

    public String getStart_time()
    {
        return start_time;
    }

    public String getTitle()
    {
        return title;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setDoc_link(String doc_link)
    {
        this.doc_link = doc_link;
    }

    public void setOrganization_ids(String organization_ids)
    {
        this.organization_ids = organization_ids;
    }

    public void setStart_time(String start_time)
    {
        this.start_time = start_time;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

}
