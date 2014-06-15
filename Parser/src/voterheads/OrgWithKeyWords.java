package voterheads;

import java.util.ArrayList;

public class OrgWithKeyWords
{
    // TODO Auto-generated method stub
    private Organization      organization;
    private ArrayList<String> keyWords;

    public ArrayList<String> getKeyWords()
    {
        return keyWords;
    }

    public Organization getOrganization()
    {
        return organization;
    }

    public void setKeyWords(ArrayList<String> keyWords)
    {
        this.keyWords = keyWords;
    }

    public void setOrganization(Organization organization)
    {
        this.organization = organization;
    }
}
