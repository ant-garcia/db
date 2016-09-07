/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.Comparator;

/**
 *
 * @author Anthony
 */
public class Tuple
{
    String key;
    String url;
    
    public Tuple(String key, String url)
    {
        this.key = key;
        this.url = url;
    }
    
    public String getKey()
    {
        return this.key;
    }
    
    public String getUrl()
    {
        return this.url;
    }
}
