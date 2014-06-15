package voterheads;

public class Organization
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // TODO Auto-generated method stub

    }

    private String  agenda_file_format;
    private String  agenda_url;
    private Boolean backend_alerts;
    private String  created_at;
    private String  description;
    private String  email_repository_sender_address;
    private String  general_url;
    private String  id;
    private Integer max_days_inactivity;
    private String  minutes_url;
    private String  name;
    private Boolean process_on_back_end;
    private String  state;
    private String  status_key;
    private String  type_id;
    private String  updated_at;
    private String  keyWords;
    private Integer search_link_depth;
    private Integer max_topics = 100;

    public String getAgenda_file_format()
    {
        return agenda_file_format;
    }

    public String getAgenda_url()
    {
        return agenda_url;
    }

    public Boolean getBackend_alerts()
    {
        return backend_alerts;
    }

    public String getCreated_at()
    {
        return created_at;
    }

    public String getDescription()
    {
        return description;
    }

    public String getEmail_repository_sender_address()
    {
        return email_repository_sender_address;
    }

    public String getGeneral_url()
    {
        return general_url;
    }

    public String getId()
    {
        return id;
    }

    public String getKeyWords()
    {
        return keyWords;
    }

    public Integer getMax_days_inactivity()
    {
        return max_days_inactivity;
    }

    public Integer getMax_topics()
    {
        return max_topics;
    }

    public String getMinutes_url()
    {
        return minutes_url;
    }

    public String getName()
    {
        return name;
    }

    public Boolean getProcess_on_back_end()
    {
        return process_on_back_end;
    }

    public Integer getSearch_link_depth()
    {
        return search_link_depth;
    }

    public String getState()
    {
        return state;
    }

    public String getStatus_key()
    {
        return status_key;
    }

    public String getType_id()
    {
        return type_id;
    }

    public String getUpdated_at()
    {
        return updated_at;
    }

    public void setAgenda_url(String agenda_url)
    {
        this.agenda_url = agenda_url;
    }

    public void setAgendaFileFormat(String agenda_file_format)
    {
        this.agenda_file_format = agenda_file_format;
    }

    public void setBackend_alerts(Boolean backend_alerts)
    {
        this.backend_alerts = backend_alerts;
    }

    public void setCreated_at(String created_at)
    {
        this.created_at = created_at;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setEmail_repository_sender_address(
            String email_repository_sender_address)
    {
        this.email_repository_sender_address = email_repository_sender_address;
    }

    public void setGeneral_url(String general_url)
    {
        this.general_url = general_url;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setKeyWords(String keyWords)
    {
        this.keyWords = keyWords;
    }

    public void setMax_days_inactivity(Integer max_days_inactivity)
    {
        this.max_days_inactivity = max_days_inactivity;
    }

    public void setMax_topics(Integer max_topics)
    {
        this.max_topics = max_topics;
    }

    public void setMinutes_url(String minutes_url)
    {
        this.minutes_url = minutes_url;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setProcess_on_back_end(Boolean process_on_back_end)
    {
        this.process_on_back_end = process_on_back_end;
    }

    public void setSearch_link_depth(Integer search_link_depth)
    {
        this.search_link_depth = search_link_depth;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public void setStatus_key(String status_key)
    {
        this.status_key = status_key;
    }

    public void setType_id(String type_id)
    {
        this.type_id = type_id;
    }

    public void setUpdated_at(String updated_at)
    {
        this.updated_at = updated_at;
    }

    @Override
    public String toString()
    {
        final String s = "agenda_url=" + agenda_url + " created_at="
                + created_at + " description=" + description + " general_url="
                + general_url + " id=" + id + " minutes_url=" + minutes_url
                + " name=" + name + "  type_id=" + type_id + " updated_at="
                + updated_at + " search_link_depth=" + search_link_depth
                + " max_topics=" + max_topics + " keywords={" + keyWords + "}";
        return s;
    }

}
