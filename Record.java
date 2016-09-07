import java.io.File;

public class Record
{
	static final int namePad = 16;
	static final int fileNamePad = 16;
	int totalKeys;
	int totalValues;
	String name;
	File file;

    public Record(){}

	public Record(String name, File f)
	{
		this.name = name;
		this.file = f;
        totalKeys = 0;
        totalValues = 0;
	}

	public static String pad(String s)
	{
		for(int i = s.length(); i < namePad; i++)
            s += " ";
        return s;
	}

	public void setTotalKeys(int keys)
	{
		this.totalKeys = keys;
	}

	public void setTotalValues(int values)
	{
		this.totalValues = values;
	}

	public int getTotalKeys()
	{
		return totalKeys;
	}

	public int getTotalValues()
	{
		return totalValues;
	}

}