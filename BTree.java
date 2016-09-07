/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.*;
import java.io.*;
/**
 *
 * @author Anthony
 */
public class BTree<T extends Comparable<T>>
{
    
    static protected int order;
    static private int maxValues;
    static private final int blockSize = 4096;
    static protected File f;// = new File("inputs.dat");
    static boolean loadedFromFile;
    Node<T> root;
    static public RandomAccessFile raf;
    static protected int numOfNodes = 0;
    static protected int lastBlockNum;
    protected int totalKeys = 0; 
    protected int totalValues = 0;
    public Comparator<T> bcmp = new Comparator<T>()
    {
        @Override
        public int compare(T a, T b)
        {
           return ((Comparable<T>)a).compareTo(b);
        }
    };
    
    public BTree(int order)throws IOException
    {
        this.order = order;
        raf = new RandomAccessFile(f,"rwd");
        maxValues = (2 * order) - 1;
        root = null;
        
    }

    public BTree(int order, File f)throws IOException
    {
        this.order = order;
        this.f = f;
        raf = new RandomAccessFile(f,"rwd");
        maxValues = (2 * order) - 1;
        numOfNodes = 0;
        root = null;
    }

    
    public void close()throws IOException
    {
        raf.close();
    }
    
    public void load()throws IOException
    {
        if(raf.length() != 0)
        {
            raf.seek(0);
            if(root == null)
                root = new Node(order, true, numOfNodes++);       
            root.diskRead();
            lastBlockNum = root.getLastBlockNum();
            root.load();
        }
    }
    
    public void reWrite()throws IOException
    {
        if(root != null)
            root.reWrite();
    }
    
    public void traverse()throws IOException
    {
        if(root != null)
        	root.traverse();
    }
    
    public boolean find(T k)throws IOException
    {
        return root.containsVal(k);
    }

	public boolean findKey(T u)throws IOException
    {
        return root.containsKey(u);
    }    
    
    public void display(T k)throws IOException
    {
        if(root != null)
            root.display(k);    
    }

    public void displayAllDataFromKey(T k)throws IOException
    {
        if(root != null)
            root.displayAllDataFromKey(k);
    }

    public void insert(T k, T u)throws IOException
    {
        if(root == null)
        {
            root = new Node(order, true, numOfNodes++);
            root.values[0] = k;
            root.keys[0] = u;
            root.numOfValues = 1;
            root.diskWrite();
        }
        else
        {
            if(root.numOfValues == maxValues)
            {
                Node<T> newNode = new Node(order, false, numOfNodes++);
                newNode.children[0] = root;
                
                newNode.splitChild(0, root);
                
                int i = 0;
                if(bcmp.compare(newNode.values[0], k) < 0)
                    i++;
                newNode.children[i].insertNonFull(k, u);
                root = newNode;
            }
            else
                root.insertNonFull(k, u);
        }
    }
    
    public T remove(T k)throws IOException
    {
        T removed = null;
                 
        if(root == null)
            return removed;
        
        removed = root.remove(k);
        
        if((root.numOfValues == 0) && (!root.isLeaf))
            root = root.children[0];
        
        return removed;
    }
    public boolean groupRemove(T k)throws IOException
    {
        Node<T> removed = root;
        while(root.searchSeq(k) != null)
            root.remove(k);
        
        return true;
    }
    
    private static class Node<T> 
    {
        private T values[];
        private T keys[];
        private boolean isLeaf;
        private boolean isLoaded;
        private boolean loaded = BTree.loadedFromFile;
        private Node<T> children[];
        private int numOfValues;
        private int numOfChildren;
        private int degree;
        private long position;
        private int blockNum;
        private int nextBlockNums[];
        final static private int sizeOfValue = 16;
        final static private int sizeOfKey = 40;
        public Comparator<T> cmp = new Comparator<T>()
        {
        @Override
        public int compare(T a, T b)
        {
           return ((Comparable<T>)a).compareTo(b);
        }
        };
        
        private Node(int order, boolean isLeaf, int blockNum)throws IOException
        {
            this.degree = order;
            this.isLeaf = isLeaf;
            isLoaded = false;
            this.blockNum = blockNum;
            position = blockNum * blockSize;
            numOfValues = 0;
            values = (T[]) new Comparable[maxValues];
            keys = (T[]) new Comparable[maxValues];
            children = new Node[maxValues + 1];
            nextBlockNums = new int[maxValues + 1];
        }
        
        private boolean diskRead()throws IOException
        {            
            if(isLoaded)
                return true;
            BTree.raf.seek(position);
            byte leafFlag;
            leafFlag = raf.readByte();
            isLeaf = (leafFlag == 1);
            numOfValues = raf.readInt();
            blockNum = raf.readInt();
            numOfChildren = raf.readInt();

            for(int i = 0; i < numOfChildren; i++)
            {
                nextBlockNums[i] = raf.readInt();
            }
            
            values = (T[]) new Comparable[maxValues];
            int i = 0;
            byte valBuf [] = new byte[sizeOfValue];
            while(i < numOfValues && i < maxValues)
            {
                raf.readFully(valBuf, 0, sizeOfValue);
                values[i] = (T)new String(valBuf);
                i++;
            }
            i = 0;
            byte keyBuf[] = new byte[sizeOfKey];
            while(i < numOfValues && i < maxValues)
            {
                raf.readFully(keyBuf, 0, sizeOfKey);
                keys[i] = (T)new String(keyBuf);
                i++;
            }
            if(numOfValues > 0 && !isLeaf)
            {   
                int amtOfChildren = 0;
                while((amtOfChildren < children.length) && (children[amtOfChildren] != null))
                    amtOfChildren++;
                long childAddresses[] = new long[amtOfChildren];
                long thisChildAddress[] = childAddresses;
                for(int j = 0; j < childAddresses.length;j++)
                    childAddresses[j] = raf.readLong();
                for(int j = 0; j < childAddresses.length; j++)
                    children[j].position = thisChildAddress[j];                
            }
            
            isLoaded = true;
            return true;
        }
        
        private boolean diskWrite()throws IOException
        {
            raf.seek(position);
            byte leafFlag = (byte)(isLeaf? 1:0);
            raf.writeByte(leafFlag);
            raf.writeInt(numOfValues);
            raf.writeInt(blockNum);
            raf.writeInt(getNumOfChildren());

            for(int i = 0; nextBlockNums[i] != 0 && i < nextBlockNums.length; i++)
            {
                raf.writeInt(nextBlockNums[i]);
            }

            int i = 0;
            while((i <= numOfValues) && (i < maxValues) && (values[i] != null))
            {
                String data = values[i].toString();
                raf.writeBytes(data);
                i++;
            }
            i = 0;
            while((i <= numOfValues) && (i < maxValues) && (keys[i] != null))
            {
                String data = keys[i].toString();
                raf.writeBytes(data);
                i++;
            }
            if((numOfValues > 0) && !isLeaf)
            {
                int numOfChildren = 0;
                while((numOfChildren < children.length) && (children[numOfChildren] != null))
                    numOfChildren++;
                long childAddresses[] = new long[numOfChildren];
                long thisChildAddress[] = childAddresses;
                i = 0;
                while((i < children.length) && (children[i] != null))
                {
                        thisChildAddress[i] = children[i].position;
                        i++;
                }
                
                long longsWritten = 0;
                for(i = 0; (i < childAddresses.length);i++)
                { 
                    raf.writeLong(childAddresses[i]);
                    longsWritten++;
                }

                if(numOfValues +1 != longsWritten)
                    return false;
            }
            return true;
        }
        
        private void free()
        {
            if(isLoaded)
            {
                for(int i = 0;(i < numOfValues);i++)
                    values[i] = null;
                for(int i = 0; (i < maxValues +1); i++)
                    children[i] = null;
                isLoaded = false;
            }
        }

        private int getNumOfChildren()
        {
            int i = 0;
            int amount = 0;
            /*for(int i = 0; i < children.length; i++)
            	if(children[i] != null)
                	amount++;*/
            while(i < children.length && children[i] != null)
            {
            	i++;
            	amount++;
            }

            return amount;
        }


        private void load()throws IOException
        {
            if(blockNum == BTree.lastBlockNum)
                return;
            if(nextBlockNums[0] == 0)
                return;

            for(int i = 0; nextBlockNums[i] != 0 && i < nextBlockNums.length; i++)
            {
                children[i] = new Node(order, true, nextBlockNums[i]);
                blockNum = nextBlockNums[i];
                children[i].diskRead();
                children[i].load();
            }

        }

        private void reWrite()throws IOException
        {
            if(blockNum == 0)
            {
                findNextBlockNums();
                diskWrite();
            }

            int i;
            for(i = 0; children[i] != null && i < children.length; i++)
                if(!isLeaf)
                {
                    children[i].findNextBlockNums();
                    children[i].diskWrite();
                    children[i].reWrite();  
                }
            
            if(!isLeaf && children[i] != null)
            {
                children[i].findNextBlockNums();
                children[i].diskWrite();
                children[i].reWrite();
            }
            
        }
        
        private void findNextBlockNums()
        {
            for(int j = 0; (children[j] != null) && (j < children.length); j++)
                nextBlockNums[j] = children[j].blockNum;
        }

        private int getLastBlockNum()
        {
            int index = 0;
            for(int i = 0; nextBlockNums[i] != 0 && i < nextBlockNums.length; i++)
                index = nextBlockNums[i];

            return index;
        }

        private void traverse()throws IOException
        { 
            int i;
            for(i = 0; i < numOfValues;i++)
            {
                if(!isLeaf)
                {
                    
                    
                    children[i].diskRead();
                    children[i].traverse();
                }
                System.out.println("|" + values[i] + " | " + keys[i] + "|");
            }
            
            if(!isLeaf)
            {
                children[i].diskRead();
                children[i].traverse();
            }
        }
        
        private void display(T k)throws IOException
        {
            int i = 0;
            for(i = 0; i < numOfValues;i++)
            {
                if(k.toString().equalsIgnoreCase(values[i].toString().substring(0, k.toString().length())))
                    System.out.println(values[i] + " " + keys[i]);
                if(!isLeaf)
                {
                    children[i].diskRead();
                    children[i].display(k);
                }
            }
            
            if(!isLeaf)
            {
                children[i].diskRead();
                children[i].display(k);
            }
        }


        private void displayAllDataFromKey(T k)throws IOException
        {
            int i = 0;
            for(i = 0; i < numOfValues; i++)
            {
                if(keys[i].toString().toLowerCase().indexOf(k.toString().toLowerCase()) >= 0)
                    System.out.println("|" + values[i] + "|");
                if(!isLeaf)
                {
                    children[i].diskRead();
                    children[i].displayAllDataFromKey(k);
                }
            }

            if(!isLeaf)
            {
                children[i].diskRead();
                children[i].displayAllDataFromKey(k);
            }
        }

        private boolean containsVal(T k)throws IOException
        {
            return search(k) != null;
        }

        private boolean containsKey(T u)throws IOException
        {
        	return searchKey(u) != null;
        }

        private Node<T> search(T k)throws IOException
        {
            int i = 0;
            
            while((i < numOfValues) && cmp.compare(k, values[i]) > 0)
                i++;
            if((i < numOfValues) && (cmp.compare(k, values[i]) == 0))
                return this;
            else if(isLeaf)
                return null;
            else
            {
                children[i].diskRead();
                return children[i].search(k);
            }
        }
        
        private Node<T> searchKey(T u)throws IOException
        {
        	int i = 0;
            
            while((i < numOfValues) && cmp.compare(u, keys[i]) > 0)
                i++;
            if((i < numOfValues) && (cmp.compare(u, keys[i]) == 0))
                return this;
            else if(isLeaf)
                return null;
            else
            {
                children[i].diskRead();
                return children[i].searchKey(u);
            }
        }

        private Node<T> searchSeq(T k)throws IOException
        {
            int i = 0;
            
            while((i < numOfValues) && (k.toString().compareToIgnoreCase(values[i].toString()) > 0))
                i++;
                if((i < numOfValues) && (k.toString().equalsIgnoreCase(values[i].toString().substring(0, k.toString().length()))))
                    return this;
                else if(isLeaf)
                    return null;
                else
                {
                    children[i].diskRead();
                    return children[i].searchSeq(k);
                }
        }
        
        private void insertNonFull(T k, T u)throws IOException
        {
            int i = numOfValues - 1;
            
            if(isLeaf)
            {
                while((i >= 0) && (cmp.compare(values[i], k) > 0))
                {
                    values[i+1] = values[i];
                    keys[i+1] = keys[i];
                    i--;
                }
                
                values[i+1] = k;
                keys[i+1] = u;
                numOfValues++;
                diskWrite();
            }
            else
            {
                while((i >= 0) && (cmp.compare(values[i], k) > 0))
                    i--;
                i++;
                children[i].diskRead();
                if(children[i].numOfValues == maxValues)
                {
                    splitChild(i, children[i]);
                    
                    if(cmp.compare(values[i], k) < 0)
                        i++;
                }
                children[i].insertNonFull(k, u);
            }
        }
        
        private void splitChild(int i, Node<T> thisNode)throws IOException
        {  
            Node<T> newNode = new Node(thisNode.degree, thisNode.isLeaf, numOfNodes++);
            newNode.numOfValues = order - 1;
            
            if(thisNode.blockNum == 0)
            {
                long newPosition = thisNode.position;
                int newBlockNum = thisNode.blockNum;
                thisNode.blockNum = blockNum;
                thisNode.position = position;
                blockNum = newBlockNum;
                position = newPosition;
                
            }
            
            for(int j = 0;j < (order-1);j++)
            {
                newNode.values[j] = thisNode.values[j+order];
                newNode.keys[j] = thisNode.keys[j+order];
                thisNode.values[j+order] = null;
                thisNode.keys[j+order] = null;
            }
            if(!thisNode.isLeaf)
                for(int j = 0; j < order;j++)
                {
                    newNode.children[j] = thisNode.children[j+order];
                    thisNode.children[j+order] = null;
                }
            thisNode.numOfValues = order - 1;
            
            for(int j = numOfValues;j >= i+1;j--)
                children[j+1] = children[j];
            children[i+1] = newNode;
            
            for(int j = numOfValues -1; j >= i;j--)
            {
                values[j+1] = values[j];
                keys[j+1] = keys[j];
            }
            values[i] = thisNode.values[order-1];
            keys[i] = thisNode.keys[order-1];
            thisNode.values[order-1] = null;
            thisNode.keys[order-1] = null;
            
            numOfValues++;
            
            diskWrite();
            thisNode.diskWrite();
            newNode.diskWrite();
        }
        
        private int findKey(T k)
        {
            int index = 0;
            
            while((index < numOfValues) && (((cmp.compare(k, values[index]) > 0) || (k.toString().compareToIgnoreCase(values[index].toString()) > 0))))
                index++;
            return index;
        }
        
        
        private T remove(T k)throws IOException
        {
            T removed = null;
            int index = this.findKey(k);
            
            if((index < numOfValues) && (((cmp.compare(values[index],k) == 0) || (k.toString().equalsIgnoreCase(values[index].toString().substring(0, k.toString().length()))))))
            {
                if(isLeaf)
                    removed = this.removeFromLeaf(index);
                else
                    removed = this.removeFromNonLeaf(index);
            }
            else
            {
                if(children[index].numOfValues < order)
                    ensure(index);
                if((index == numOfValues) && (index > numOfValues))
                {
                    children[index-1].diskRead();
                    children[index-1].remove(k);
                }
                else
                {
                    children[index].diskRead();
                    children[index].remove(k);
                }
            }     
            return removed;
        }
        
        private T removeFromLeaf(int i)throws IOException
        {
            T removed = values[i];
            
            for(int j = i+1;j < numOfValues; ++j)
            {
                values[j-1] = values[j];
                keys[j-1] = keys[j];
            }
            values[numOfValues -1] = null;
            keys[numOfValues -1] = null;
            numOfValues--;
            diskWrite();
            return removed;
        }
        
        
        private T removeFromNonLeaf(int i)throws IOException
        {
            T removed = values[i];
            Node<T> child = children[i];            
            Node<T> sibling = children[i+1];
            
            child.diskRead();
            sibling.diskRead();
            
            if(child.numOfValues >= order)
            {
                T p = getPredecessor(i);
                values[i] = p;
                child.remove(p);
            }
            else if(sibling.numOfValues >= order)
            {
                T s = getSuccessor(i);
                values[i] = s;
                sibling.remove(s);
            }
            else
            {
                combine(i);
                child.remove(removed);
            }
            
            return removed;
        }
        
        private void ensure(int i)throws IOException
        {
            if((i != 0) && (children[i-1].numOfValues >= order))
                retrieveFromPrevious(i);
            else if((i != numOfValues) && (children[i+1].numOfValues >= order))
                retrieveFromNext(i);
            else
            {
                if(i != numOfValues)
                    combine(i);
                else
                    combine(i-1);
            }
        }
        
        private void combine(int i)throws IOException
        {
            Node<T> child = children[i];
            Node<T> sibling = children[i+1];
            child.values[order-1] = values[i];
            child.keys[order-1] = keys[i];
            child.diskRead();
            sibling.diskRead();
            
            for(int j = 0; j < sibling.numOfValues; ++j)
            {
                child.values[j+order] = sibling.values[j];
                child.keys[j+order] = sibling.keys[j];
            }
            child.numOfValues += sibling.numOfValues +1;
            
            if(!child.isLeaf)
                for(int j = 0;j <= sibling.numOfValues;++j)
                    child.children[j+order] = sibling.children[j];
            
            for(int j = i+1;j < numOfValues;++j)
            {
                values[j-1] = values[j];
                keys[j-1] = keys[j];
                children[j] = children[j+1];
            }
            values[numOfValues -1] = null;
            keys[numOfValues -1] = null;
            children[numOfValues] = null;
            numOfValues--;
            
            diskWrite();
            child.diskWrite();
            sibling.free();
        }
        
        
        private T getPredecessor(int i)throws IOException
        {
            Node<T> current = children[i];
            current.diskRead();
            
            while(!current.isLeaf)
                current = current.children[current.numOfValues];
            return current.values[current.numOfValues -1];
        }
        
        private T getSuccessor(int i)throws IOException
        {
            Node<T> current = children[i+1];
            current.diskRead();
            
            while(!current.isLeaf)
                current = current.children[0];
            return current.values[0];
        }
        
        private void retrieveFromPrevious(int i)throws IOException
        {
            Node<T> child = children[i];
            Node<T> sibling = children[i-1];
            
            child.diskRead();
            sibling.diskRead();
            
            for(int j = child.numOfValues -1; j >= 0;--j)
            {
                child.values[j+1] = child.values[j];
                child.keys[j+1] = child.keys[j];
            }
            
            if(!child.isLeaf)
                for(int j = child.numOfValues;j >= 0; --j)
                    child.children[j+1] = child.children[j];
            
            child.values[0] = values[i-1];
            child.keys[0] = keys[i-1];
            
            if(!isLeaf)
                child.children[0] = sibling.children[sibling.numOfValues];
            
            values[i-1] = sibling.values[sibling.numOfValues -1];
            keys[i-1] = sibling.keys[sibling.numOfValues -1];
            
            child.numOfValues++;
            sibling.numOfValues--;
            
            diskWrite();
            child.diskWrite();
            sibling.diskWrite();
        }
        
        private void retrieveFromNext(int i)throws IOException
        {
            Node<T> child = children[i];
            Node<T> sibling = children[i+1];
            
            child.diskRead();
            sibling.diskRead();
            
            child.values[child.numOfValues] = values[i];
            child.keys[child.numOfValues] = keys[i];
            values[i] = sibling.values[0];
            keys[i] = sibling.keys[0];
            
            if(!child.isLeaf)
                child.children[child.numOfValues] = sibling.children[0];
            
            for(int j = 1;j < sibling.numOfValues;++j)
            {
                sibling.values[j-1] = sibling.values[j];
                sibling.keys[j-1] = sibling.keys[j];
            }
            sibling.values[sibling.numOfValues -1] = null;
            sibling.keys[sibling.numOfValues -1] = null;
            
            if(!sibling.isLeaf)
                for(int j=1;j <= sibling.numOfValues;++j)
                    sibling.children[j-1] = sibling.children[j];
            sibling.children[sibling.numOfValues] = null;
            
            child.numOfValues++;
            sibling.numOfValues--;
            
            diskWrite();
            child.diskWrite();
            sibling.diskWrite();
        }        
    }
}
