import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;  
import java.io.FileNotFoundException;
import java.io.FileOutputStream;  
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.zip.GZIPInputStream;  

  

class IndexBuilder
{
	static WordMap index = new WordMap();
    static int document_ID = 0;
    static int invertedNum=0;
    static String fn;
//	static int totalFileNum=1801111;
//    static int totalFileNum=638012;
    static int totalFileNum=1855670;
	public static void main(String[] args) throws IOException{  
		//removeFiles();
		//add a loop read all the files(data --- index)
		for(int fileNum=0; fileNum< totalFileNum; )
		{
			index.postingMap = new TreeMap<String,TreeMap<Integer,Integer>> ();
			// take every 300 xml files as a file
			for(int i=0; i<300; i++){
				if(fileNum >= totalFileNum)
					break;
				String zero = "0000000";
				String tmp = Integer.toString(fileNum);
				//String tmp = "0001000";//just for test
				tmp = zero.substring(0, 7-tmp.length())+tmp;
				fn = tmp;
				String filename = "/mnt/hgfs/ubuntu_share-2/workspace/ExtraFile/data/all/"+tmp+".xml";
				System.out.println(filename);
				parse(filename);//parse a page and insert into postings
				fileNum++;
//				if(fileNum != document_ID)
//				{
//					System.out.println("not equal");
//					System.out.println("fileNum: "+fileNum+"  document_ID: "+document_ID);
//					return ;
//				}
			}

			//print inverted list into disk(300 # of xml)
			invertedIndexWriter(invertedNum);
			invertedNum++;
		}
		linuxSort();
		lexiconIndexWriter("lexicon_index.txt");
		urlIndexWriter("url_index.txt");
		locationIndexWriter("location_index.txt");
		System.out.println("done");
	}
	public static void removeFiles() throws IOException{
		String command = new String("rm -r /mnt/hgfs/ubuntu_share/workspace/NewYorkTime/result/inverted_index*.txt");
    	//System.out.println(command);
		Process cmdProc = Runtime.getRuntime().exec(command);
	}

    public static void parse(String fileName){
        NYTCorpusDocument doc = new NYTCorpusDocument();
        NYTCorpusDocumentParser docParser = new NYTCorpusDocumentParser();
        float rank=-1;
        try{
        	doc = docParser.parseNYTCorpusDocumentFromFile(new File(fileName),false);
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        	return;
        }
        if(doc == null)
        	return;
        else if(doc.body == null)
        	return;
        //extract location data
        if(doc.onlineLocations.size() > 0)
        {
        	//parse the location
        	for(int i=0; i<doc.onlineLocations.size(); i++)
        	{
        		String city = doc.onlineLocations.get(i).toLowerCase();
        		if(city.equals(""))
        			continue;
        		ArrayList<Integer> docID = index.locationMap.get(city);
        		if(docID == null)
        			docID = new ArrayList<Integer>();
        		docID.add(document_ID);
        		index.locationMap.put(city.replace("\n", " "), docID);
        	}
        }
        else 
        {
        	if(doc.locations.size() > 0)
        	{
        		//parse the location
            	for(int i=0; i<doc.locations.size(); i++)
            	{
            		String city = doc.locations.get(i).toLowerCase();
            		if(city.equals(""))
            			continue;
            		ArrayList<Integer> docID = index.locationMap.get(city);
            		if(docID == null)
            			docID = new ArrayList<Integer>();
            		docID.add(document_ID);
            		index.locationMap.put(city.replace("\n", " "), docID);
            	}
        	}
        }
        //parse every world in body
		String[] content=doc.body.split("\n");
		for (String line:content){
			String[] words=line.split(" ");
			for(String word:words)
			{
				word = word.toLowerCase();
				if(word.matches("[a-z0-9]+"))//only store word only contain [a~z]
				{
					index.inSertIntoPostingMap(word, document_ID);//insert into 
				}
			}
		}
		index.urlDocMap.put(document_ID, new UrlDocLen(doc.url.toString(), doc.body.length(), rank, fn));//it's # of unicode not the length!!!!!!
		document_ID++;
	}//end for a document




	public static void invertedIndexWriter(int fileNum){
		System.out.println("start write inverted index into file");
		//start print Inverted index to file

		Iterator  ilter1= index.postingMap.entrySet().iterator();
		BufferedWriter fout;
		try {
			fout = new BufferedWriter(new FileWriter("result/inverted_index_"+fileNum+".txt"));
			while (ilter1.hasNext())
			{
				 Map.Entry entry1 = (Map.Entry) ilter1.next();
				 String word = (String) entry1.getKey();
				 TreeMap posting = (TreeMap) entry1.getValue();

				 Iterator  ilter2= posting.entrySet().iterator();
				 String post_string = word+" ";
				 while (ilter2.hasNext())  // concatenate all docID and freq for this word;
				 {
					 Map.Entry entry2 = (Map.Entry) ilter2.next();
					 int docID = (int)entry2.getKey();
					 int freq = (int)entry2.getValue();
					 post_string += docID+" "+freq+" ";
				 }
				 fout.write(post_string+"\n");
			 }
			fout.flush();
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//end print Inverted index into file inverted_index_i.txt(i from 0 to 82)
		System.out.println("end write inverted index into file");
   }//end for documents


    public static void linuxSort() throws IOException{
		//merge inverted index
    	//linux sort all the files
    	//generate linux sort command
    	int sortCount = ((invertedNum-1)%100==0)?((invertedNum-1)/100):((invertedNum-1)/100+1);
    	System.out.println("invertedNum="+invertedNum+"    "+"       sortCount="+sortCount);
    	for(int i=0; i<sortCount; i++)
    	{
    		String command = new String("sort -k1,1d -k2,2n");
    		for(int j=0; j<100; j++)
    		{
    			if((invertedNum-1)<i*100+j)
    				break;
    			command += " result/inverted_index_"+(i*100+j)+".txt";
    		}
	    	//System.out.println(command);
			Process cmdProc = Runtime.getRuntime().exec(command);
			BufferedWriter fout = new BufferedWriter(new FileWriter("result/tmp_index_"+i+".txt"));
			BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()));
			String line;
			while ((line = stdoutReader.readLine()) != null) {
				fout.write(line+"\n");
			}
			fout.close();
    	}
    	String command = new String("sort -k1,1d -k2,2n");
    	for(int i=0; i<sortCount; i++)
    	{
    		command += " result/tmp_index_"+i+".txt";
    	}
    	//System.out.println(command);
		Process cmdProc = Runtime.getRuntime().exec(command);
		OutputStream fout = new BufferedOutputStream(new FileOutputStream("0"),128*1024);
		BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()));
		String line, previousWord;

		int offset=0,startOffset=0,filename=0,chunkNum=0,wordTotalNum=0,docFreq;
//		List<Byte> chunk = new ArrayList<Byte>(256);
//		int [] chunk;
		String word[]={"0"};//initialize the first word;
		List<Byte> compressedChunk ;
		List<Integer> docIDsInList=new ArrayList<Integer>();
		List<Integer> freqsInList=new ArrayList<Integer>();
		List<Byte> compressedList = new ArrayList<Byte>();
		int termID = 0;
		int totalFreq = 0;
//		docIDsInList.add(0);freqsInList.add((byte)0);//initialize the lists for the first check in the loop;
		//read lines after merge
		while ((line = stdoutReader.readLine()) != null) {
			previousWord=word[0];
			//System.out.println(line);
			word=line.split(" "); //word[0]=word,word[1]=docId,word[2]=freq,word[3]=docId, word[4]=freq,....
			if(!previousWord.equals(word[0])){// if we read a new word, we make up the inverted list of the last word;
				//make chunks for the previous word;
				docFreq=docIDsInList.size(); // the docID number of the given word;
				chunkNum=(docFreq%128==0)?docFreq/128:docFreq/128+1;
				startOffset=offset;
				List<Byte> metadata = new ArrayList<Byte>();
				int lastDocID = 0;
				int preDocID = 0;//docID in metadata
				//compress every chunk(docID , freq)
				for(int i=0; i<chunkNum; i++)
				{
					if((i+1)*128 > docFreq)
					{
						compressedChunk = new ArrayList<Byte>();
						//compress the first docID,Freq and add into compressedChunk
						int firsstDocID = docIDsInList.get(i*128);
						if(lastDocID != 0)
							firsstDocID = firsstDocID - lastDocID;
						compressedChunk.addAll(VB.VB_Compress(firsstDocID));
						compressedChunk.addAll(VB.VB_Compress(freqsInList.get(i*128)));
						
						for (int j=i*128+1;j<docFreq;j++){
							//compress docID and add into compressedChunk
							compressedChunk.addAll(VB.VB_Compress(docIDsInList.get(j)-docIDsInList.get(j-1)));
							//compress freq and add into compressedChunk
							compressedChunk.addAll(VB.VB_Compress(freqsInList.get(j)));
						}
						//compress the length of the chunk and add into metadata
						lastDocID = docIDsInList.get(docFreq - 1);
						//metadata store the last docID and length(bytes) of the chunk
						metadata.addAll(VB.VB_Compress(lastDocID - preDocID));
						preDocID = lastDocID;
						metadata.addAll(VB.VB_Compress(compressedChunk.size()));  
						//add compressed chunk into compressedList(contain all the compressed data except metadata)
						compressedList.addAll(compressedChunk);
					}
					else
					{
						compressedChunk = new ArrayList<Byte>();
						//compress the first docID,Freq and add into compressedChunk
						int firsstDocID = docIDsInList.get(i*128);
						if(lastDocID != 0)
							firsstDocID = firsstDocID - lastDocID;
						compressedChunk.addAll(VB.VB_Compress(firsstDocID));
						compressedChunk.addAll(VB.VB_Compress(freqsInList.get(i*128)));
						
						for (int j=i*128+1;j<(i+1)*128;j++){
							//compress docID and add into compressedChunk
							compressedChunk.addAll(VB.VB_Compress(docIDsInList.get(j)-docIDsInList.get(j-1)));
							//compress freq and add into compressedChunk
							compressedChunk.addAll(VB.VB_Compress(freqsInList.get(j)));
						}
						lastDocID = docIDsInList.get((i+1)*128-1);
						//compress the length of the chunk and add into metadata
						//metadata store the first docID and length(bytes) of the chunk
						metadata.addAll(VB.VB_Compress(lastDocID - preDocID));
						preDocID = lastDocID;
						metadata.addAll(VB.VB_Compress(compressedChunk.size()));  
						//add compressed chunk into compressedList(contain all the compressed data except metadata)
						compressedList.addAll(compressedChunk);
					}
				}
				offset+=metadata.size() + compressedList.size();//set the offset
				/***
				 * write chunks into file
				 */
//				byte[] tmp = (byte[])metadata.toArray(new byte[metadata.size()]);
				//1.write compressed metadata 
				for(int i=0; i<metadata.size(); i++)
				{
					fout.write(metadata.get(i));
				}
			
				// 2.write compressed docId chunks
				for(int i=0; i<compressedList.size(); i++){
					fout.write(compressedList.get(i));
				}
				
				/*insert the lexinfo to lexicon map*/
				//term, termID, fileName, totalFreq, offset, docFreq, metadataSize, length
				
				index.inSertIntoLexMap(previousWord,termID, filename, totalFreq, startOffset, docFreq, metadata.size(), offset-startOffset);
				termID ++;
				totalFreq = 0;
				/*check out inverted index file*/
				wordTotalNum++;
				if ((wordTotalNum&0xFFFFF)==0xFFFFF){// store 4096*16 words(inverted lists) in one index file;
					System.out.println("next file");
					offset=0;
					fout.flush();//force to write out the buffer;
			        fout.close();
			        filename=wordTotalNum;// using words number as the name of index file gives more convenience for observation;
					fout = new BufferedOutputStream(new FileOutputStream(Integer.toString(filename)),128*1024);
				}
				//make new docIDsInList and freqsInList
				docIDsInList=new ArrayList<Integer>();
				freqsInList=new ArrayList<Integer>();
				compressedList = new ArrayList<Byte>();
				for (int i=0;i<((word.length-1)>>>1);i++){
					docIDsInList.add(Integer.parseInt(word[i*2+1]));
					freqsInList.add((int)Integer.parseInt(word[i*2+2]));
					totalFreq += (int)Integer.parseInt(word[i*2+2]);
				}
			}else{// when the posting belonging to the same word, add them into doc id list and freq list;
				for (int i=0;i<((word.length-1)>>>1);i++){
					docIDsInList.add(Integer.parseInt(word[i*2+1]));
					freqsInList.add((int)Integer.parseInt(word[i*2+2]));
					totalFreq += (int)Integer.parseInt(word[i*2+2]);
				}
			}
		}
		//TODO write the last word!!
        fout.close();

		//print error of linux sort
		BufferedReader stderrReader = new BufferedReader(
		         new InputStreamReader(cmdProc.getErrorStream()));
		while ((line = stderrReader.readLine()) != null) {
			System.out.println(line);
		}

		int retValue = cmdProc.exitValue();
		//System.out.println(retValue);
	}

    
	public static void lexiconIndexWriter(String filename){
		//start print lexicon index to file
		System.out.println("start write lexicon index into file");
		Iterator ilter1= index.lexiconMap.entrySet().iterator();
		String theWord;
		try {
//			OutputStreamWriter fout2 = new OutputStreamWriter(new FileOutputStream("result/lexicon_index.txt"));
			//BufferedWriter fout2 = new BufferedWriter(new FileWriter("result/lexicon_index.txt"));
			BufferedWriter fout2 = new BufferedWriter(new FileWriter(filename));
			while (ilter1.hasNext())
	        {
				String lexicon_string = new String();//store every line of url_index
				Map.Entry entry1 = (Map.Entry) ilter1.next();
				theWord = (String) entry1.getKey();
	            int[] lexinfo = (int[]) entry1.getValue();//term frequence and offset of inverted file
	          //term, termID, fileName, totalFreq, offset, docFreq, metadataSize, length
	            lexicon_string = theWord+" "+lexinfo[0]+" "+lexinfo[1]+" "+lexinfo[2]+" "+lexinfo[3]+" "+lexinfo[4]+" "+lexinfo[5]+" "+lexinfo[6];
	            fout2.write(lexicon_string+"\n");
	        }
			fout2.flush();
			fout2.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}//end print url_index to disk
//        System.out.println("done");
        //end loop
	}



	public static void urlIndexWriter(String filename){
	//start print url index to file
		System.out.println("start write url index into file");
		Iterator ilter1= index.urlDocMap.entrySet().iterator();
		try {
			//BufferedWriter fout2 = new BufferedWriter(new FileWriter("result/url_index.txt"));
			BufferedWriter fout2 = new BufferedWriter(new FileWriter(filename));
			fout2.write((document_ID)+"\n");
			while (ilter1.hasNext())
	        {
				String url_string = new String();//store every line of url_index
				Map.Entry entry1 = (Map.Entry) ilter1.next();
	            int id = (int) entry1.getKey();
	            UrlDocLen url = (UrlDocLen) entry1.getValue();
				url_string += id+" "+url.url+" "+url.docLen+" "+url.fileName; 
//				System.out.println(url_string);
	            fout2.write(url_string+"\n");
	        }
			fout2.flush();
			fout2.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}//end print url_index to disk

    }  
	
	public static void locationIndexWriter(String filename){
		System.out.println("start write location index into file");
		//start print Inverted index to file

		Iterator  ilter1= index.locationMap.entrySet().iterator();
		BufferedWriter fout;
		try {
			fout = new BufferedWriter(new FileWriter(filename));
			while (ilter1.hasNext())
			{
				 Map.Entry entry1 = (Map.Entry) ilter1.next();
				 String city = (String) entry1.getKey();
				 ArrayList docIDs = (ArrayList) entry1.getValue();

				 String post_string = city+"\n";
				 for(int i=0; i<docIDs.size(); i++)  // concatenate all docID and freq for this word;
				 {
					 post_string += docIDs.get(i)+" ";
				 }
				 fout.write(post_string+"\n");
			 }
			fout.flush();
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("end write location index into file");
   }

	
}