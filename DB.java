import java.io.*;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;

public class DB 
{

    public static final int lengthOfValue = 16;
    public static final int lengthOfKey = 40;
    public static boolean usedFile = false;
    public Iterator it;
    public ArrayList<Record> records = new ArrayList();
    Formats f = new Formats();


    public BTree fileInput(BTree t,String url)throws IOException
    {
        String thisVal;
        String line [];
        String thisUrl = url;
        thisUrl = f.padKey(thisUrl);
        Document body = Jsoup.connect(url).timeout(100000).get();
        String text = body.body().text();
        line = text.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");

        t.totalKeys++;
        for(int i = 0; i < line.length;i++)
        {
            thisVal = line[i];
            if((thisVal.length() >= 4) && (thisVal.length() <= lengthOfValue))
            {
                thisVal = f.padVal(thisVal);
                if(t.root == null)
                {
                    t.insert(thisVal,thisUrl);
                    t.totalValues++;
                }
                else if(!t.find(thisVal))
                {
                    t.insert(thisVal,thisUrl);
                    t.totalValues++;
                }
            }
        }

        return t;
    }

    public static void main(String[] args)throws IOException 
    {
        DB db = new DB();
        Scanner fw = null;
        //File f = new File("records/");
        System.out.println("Create Record");
        //db.loadRecords();
        //db.it = db.records.iterator();

        File file = new File("tests/");
        File files[] = file.listFiles();
        for(File f : files)
        {
            fw = new Scanner(f);
            String name = fw.nextLine();
            System.out.println(name);
            db.records.add(db.createRecord("records/" + name + ".dat" + " " + name + " " + "data/" + name + "data.dat"));
            BTree<String> t = new BTree(32, new File("data/" + name + "data.dat"));
            while(fw.hasNext())
            {
                String url = fw.nextLine();
                System.out.println("Current Url: " + url);
                db.fileInput(t,url);
            }
            //t.traverse();
            t.reWrite();
            db.records.get(db.records.size() - 1).setTotalKeys(t.totalKeys);
            db.records.get(db.records.size() - 1).setTotalValues(t.totalValues);
            db.updateRecord("records/" + name + ".dat" + " " + name + " " + "data/" + name + "data.dat", t.totalKeys, t.totalValues);
            fw.reset();
        }
        fw.close();
        usedFile = true;

        //if (f.length() > 0)
            //t.load();

        Scanner in = new Scanner(System.in);
        System.out.println("Welcome to the Database \n");
        db.processDB(in, db.records);
    }

    public void loadRecords()throws IOException
    {
        Scanner lr = new Scanner(System.in);
        System.out.println("Load from file or terminal?");
        String choice = lr.nextLine();

        switch(choice)
        {
            case "file":
                System.out.println("loading files...");
                File file = new File("records/");
                File files[] = file.listFiles();
                for(File f : files)
                {   
                    String input = f.getName();
                    records.add(loadRecord(input));
                }
                break;
            case "terminal":
                System.out.println("Enter a file to store the new Record, "
                                    + "a name for the Record and a .dat file " +
                                    "to store the data");
                String createRec = lr.nextLine();
                records.add(createRecord(createRec)); 
                break;
            default:
                System.out.println("Incorrect input");
        }
    }

    public void updateRecord(String input, int totalKeys, int totalValues)throws IOException
    {
        String inputs[] = input.split("\\s+");
        File recFile = new File(inputs[0]);
        String name = Record.pad(inputs[1]);
        String fileName = Record.pad(inputs[2]);
        BufferedWriter fw = new BufferedWriter(new FileWriter(recFile, false));
        for(int i = 1; i < inputs.length; i++)
        {
            fw.write(inputs[i]);
            fw.write("\r\n");
        }

        fw.write(new Integer(totalKeys).toString());
        fw.write("\r\n");
        fw.write(new Integer(totalValues).toString());
        fw.write("\r\n");
        fw.flush();
    }

    public void displayRecords()
    {
        int pad = 0;
        it = records.iterator();
        while(it.hasNext())
        {
            Record temp = (Record) it.next();
            pad = f.formatRecordStatusBar(temp).length();
            System.out.println(f.formatRecordBar(pad));
            System.out.println(f.formatRecordStatusBar(temp));
        }
        System.out.println(f.formatRecordBar(pad));
    }

    public Record loadRecord(String input)throws IOException
    {
        String name = "";
        File treeFile = null;
        File f = new File("records/" + input);
        Scanner reader = new Scanner(f);
        Record r = new Record();

        while (reader.hasNext())
        {
            r.name = reader.nextLine();
            r.file = new File(reader.nextLine());
            r.setTotalKeys(new Integer(reader.nextLine()));
            r.setTotalValues(new Integer(reader.nextLine()));
        }

        /*byte nameBuf[] = new byte[Record.namePad];
        byte fileNameBuf[] = new byte[Record.fileNamePad];
        RandomAccessFile raf = new RandomAccessFile(f, "rwd");
        raf.readFully(nameBuf);
        String name = new String(nameBuf);
        raf.readFully(fileNameBuf);
        File treeFile = new File(new String(fileNameBuf));*/

        return r;
    }

    public Record createRecord(String input)throws IOException
    {
        String inputs[] = input.split("\\s+");
        File recFile = new File(inputs[0]);
        String name = Record.pad(inputs[1]);
        String fileName = Record.pad(inputs[2]);
        BufferedWriter fw = new BufferedWriter(new FileWriter(recFile, false));

        for(int i = 1; i < inputs.length; i++)
        {
            fw.write(inputs[i]);
            fw.write("\r\n");
        }

        fw.write(0);
        fw.write(0);
        fw.flush();

        /*RandomAccessFile raf = new RandomAccessFile(recFile, "rwd");
        raf.writeBytes(name);        
        raf.writeBytes(fileName);*/

        return new Record(inputs[1], new File(inputs[2]));
    }

    public BTree loadIndex(String name)throws IOException
    {
        //BTree t = null;
        for(Record r : records)
        {
            if(r.name.equalsIgnoreCase(name))
                return new BTree<String>(32, r.file);
        }

        return null;
    }

    public void processDB(Scanner in, ArrayList<Record> set)throws IOException
    {
        String key = null;
        BTree tree = loadIndex(set.get(0).name);
        if(!usedFile)
            tree.load();
        for(;;)
        {
            System.out.println("Please select an option or type help.");
            String input = in.nextLine().toLowerCase();
            switch(input)
            {
                case "help":
                    System.out.println("Supported options: insert, select," +
                            " remove, view");
                    break;

                case "insert":
                    if(key == null)
                    {
                        System.out.println("Enter a key and value");
                        String keyAndValue = in.nextLine();
                        tree.insert(keyAndValue.substring(keyAndValue.indexOf("")+1), keyAndValue.substring(0, keyAndValue.indexOf("")));
                    }
                    else
                    {
                        System.out.println("Enter a value");
                        String value = in.nextLine();
                        tree.insert(value, key);
                    }
                    break;
                case "select":
                    System.out.println("Enter record for record or key for key");
                    input = in.nextLine().toLowerCase();
                    if(input.equals("record"))
                    {
                        System.out.println("Enter a record to be used");
                        input = in.nextLine().toLowerCase();
                        tree = loadIndex(input);
                        tree.load();
                    }    
                    if(input.equals("key"))
                    {
                        if(tree == null)
                            System.out.println("Please load a record first");
                        else
                        {
                            System.out.println("Enter a key to be used");
                            key = in.nextLine();    
                        }
                    }
                    break;
                case "remove":
                    System.out.println("Enter a value to be removed");
                    String val = in.nextLine();
                    tree.remove(val);
                    break;
                case "view":
                    System.out.println("Enter 'record' to view the current records loaded\n" +
                            "'full' to display entire index in record\n" +
                            "'key' to display all values from key\n" +
                            "or 'find' to see if the db contains the value");
                    String choice = in.nextLine().toLowerCase();
                    if(choice.equals("record"))
                        displayRecords();
                    if(choice.equals("full"))
                    	f.packFullTraverse(tree);
                    else if(choice.equals("key"))
                    {
                        if(key != null)
                        	f.packKeyTraverse(key, tree);
                        else
                        {
                            System.out.println("Enter a key to be displayed");
                            String k = in.nextLine();
                            f.packKeyTraverse(k, tree);
                        }
                    }
                    else if(choice.equals("find"))
                    {
                        System.out.println("Enter a value to be found");
                        String v = in.nextLine();
                        if(tree.find(v))
                            System.out.println("In database");
                        else
                            System.out.println("Not Found");
                    }
                    break;
                case "quit":
                	System.out.println("Closing db");
                	tree.close();
					return;
                default:
                    System.out.println("Incorrect option");
                    break;
            }
            tree.reWrite();
        }
    }
}
