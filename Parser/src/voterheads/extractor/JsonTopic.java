package voterheads.extractor;

public class JsonTopic
{

    String order;
    String name;
    String description;
    String topic_type;
    String entity_ordinal;
    String parent;

    public String getDescription()
    {
        return description;
    }

    public String getEntity_ordinal()
    {
        return entity_ordinal;
    }

    public String getName()
    {
        return name;
    }

    public String getOrder()
    {
        return order;
    }

    public String getParent()
    {
        return parent;
    }

    public String getTopic_type()
    {
        return topic_type;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setEntity_ordinal(String entity_ordinal)
    {
        this.entity_ordinal = entity_ordinal;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setOrder(String order)
    {
        this.order = order;
    }

    public void setParent(String parent)
    {
        this.parent = parent;
    }

    public void setTopic_type(String topic_type)
    {
        this.topic_type = topic_type;
    }

}
