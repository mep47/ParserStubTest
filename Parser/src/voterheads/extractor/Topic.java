package voterheads.extractor;

public class Topic
{

    String entityOrdinal;
    String name;
    String description;
    int    order;
    int    parentOrderNumber;

    public String getDescription()
    {
        return description;
    }

    public String getEntityOrdinal()
    {
        return entityOrdinal;
    }

    public String getName()
    {
        return name;
    }

    public int getOrder()
    {
        return order;
    }

    public int getParentOrderNumber()
    {
        return parentOrderNumber;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setEntityOrdinal(String entityOrdinal)
    {
        this.entityOrdinal = entityOrdinal;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setOrder(int order)
    {
        this.order = order;
    }

    public void setParentOrderNumber(int parentOrderNumber)
    {
        this.parentOrderNumber = parentOrderNumber;
    }

    @Override
    public String toString()
    {
        final StringBuffer str = new StringBuffer(200);

        str.append("entityOrdinal: " + entityOrdinal + "\n");
        str.append("name: " + name + "\n");
        str.append("order: " + order + "\n");
        str.append("parentOrderNumber: " + parentOrderNumber + "\n");
        str.append("description: " + description + "\n");

        return str.toString();
    }

}
