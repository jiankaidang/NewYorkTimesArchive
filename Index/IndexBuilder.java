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
    static int document_ID = 1;
	static int totalFileNum=233;
	
	public static void main(String[] args) throws IOException{  
			
		//add a loop read all the files(data --- index)
		int invertedNum=0;
		for(int fileNum=0; fileNum< totalFileNum; fileNum++)
		{
			
			index.postingMap = new TreeMap<String,TreeMap<Integer,Integer>> ();
			
			//should have for(int j...... j<100)
			String zero = "0000000";
			String tmp = Integer.toString(fileNum);
			tmp = zero.substring(0, 7-tmp.length())+tmp;
			String filename = "data/"+tmp+".xml";
			parse(filename);//parse a page and insert into postings
			//end for
			
			//print inverted list into disk(100 # of xml)
			invertedIndexWriter(invertedNum);
			invertedNum++;
		}
		linuxSort();
		urlIndexWriter1("result/lexicon_index.txt");
		urlIndexWriter1("result/url_index.txt");
	}


    public static void parse(String fileName){
        NYTCorpusDocument doc = new NYTCorpusDocument();
        NYTCorpusDocumentParser docParser = new NYTCorpusDocumentParser();
        doc = docParser.parseNYTCorpusDocumentFromFile(new File(fileName),false);
		String[] content=doc.body.split("\n");
		for (String line:content){
			//System.out.println(word);
			String[] words=line.split(" ");
			for(String word:words)
			{
				word = word.toLowerCase();
				if(word.matches("[a-z0-9]+"))//only store word only contain [a~z]
				{
//					System.out.println(word);
					index.inSertIntoPostingMap(word, document_ID);//insert into 
				}
			}
		}
		index.urlDocMap.put(document_ID, new UrlDocLen(doc.url.toString(), doc.wordCount));//it's # of words not the length!!!!!!
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
    	int sortCount = ((totalFileNum-1)%100==0)?((totalFileNum-1)/100):((totalFileNum-1)/100+1);
    	System.out.println(sortCount);
    	for(int i=0; i<sortCount; i++)
    	{
    		String command = new String("sort -k1,1d -k2,2n");
    		for(int j=0; j<100; j++)
    		{
    			if((totalFileNum-1)<i*100+j)
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
		int[] chunk ;
		String word[]={"1"};//initialize the first word;
		byte[] metadata, compressedChunk = null;
		List<Integer> docIDsInList=new ArrayList<Integer>();
		List<Byte> freqsInList=new ArrayList<Byte>();
		List<byte[]> compressedList = new ArrayList<byte[]>(4);
//		docIDsInList.add(0);freqsInList.add((byte)0);//initialize the lists for the first check in the loop;
		//read lines after merge
		while ((line = stdoutReader.readLine()) != null) {
			previousWord=word[0];
			System.out.println(line);
			word=line.split(" "); //word[0]=word,word[1]=docId,word[2]=freq,word[3]=docId, word[4]=freq,....
//			temp_lexinfo=index.lexiconMap.get(word[0]);    		
			if(!previousWord.equals(word[0])){// if we read a new word, we make up the inverted list of the last word;
//				index.inSertIntoLexMap(word[0]); //insert the current word into lexiconMap immediately;
				//make chunks for the previous word;
				docFreq=docIDsInList.size(); // the docID number of the given word;
				chunkNum=(docFreq%128==0)?docFreq/128:docFreq/128+1;
				startOffset=offset;
				metadata = new byte[chunkNum];//should edit
				offset += chunkNum;//should edit
				for(int i=0; i<chunkNum; i++)
				{
					if((i+1)*128 > docFreq)
					{
						chunk=new int[docFreq-i*128];

						chunk[0]=docIDsInList.get(i*128);
						for (int j=i*128+1;j<docFreq;j++){
							chunk[j-i*128]=docIDsInList.get(j)-docIDsInList.get(j-1);
						}
						compressedChunk = VB.VBENCODE(chunk);
						//should store the compressedChunk
						compressedList.add(compressedChunk);
						offset+=compressedChunk.length;
						metadata[i]=(byte)compressedChunk.length;  //metadata store the length(bytes) of the chunk
						                                           //(bug) if length = 256, metadata become 0
//						lengthByBytes+=metadata[i];
					}
					else
					{
						chunk=new int[128];
						chunk[0]=docIDsInList.get(i*128);
						for (int j=i*128+1;j<(i+1)*128;j++){
							chunk[j-i*128]=docIDsInList.get(j)-docIDsInList.get(j-1);
						}
						compressedChunk = VB.VBENCODE(chunk);
						//should store the compressedChunk
						compressedList.add(compressedChunk);
						offset+=compressedChunk.length;
						metadata[i]=(byte)compressedChunk.length;  //metadata store the length(bytes) of the chunk
//						lengthByBytes+=metadata[i];
					}
				}
				offset+=freqsInList.size();// add the size of freq chunks;
				/***
				 * write chunks into file
				 */
				fout.write(metadata);// 1. write meta data;
				for(int i=0; i<compressedList.size(); i++){
					fout.write(compressedList.get(i));// 2.write compressed docId chunks
				}
				byte[] termfreqs = new byte[freqsInList.size()];
				for(int i=0; i<freqsInList.size(); i++){ 
					termfreqs[i] = freqsInList.get(i).byteValue();
				}
				fout.write(termfreqs); //3.write doc frequency right after docId chunks
				/*insert the lexinfo to lexicon map*/
				index.inSertIntoLexMap(previousWord,docFreq,filename,startOffset,offset-startOffset,chunkNum);
				/*check out inverted index file*/
				wordTotalNum++;
				if ((wordTotalNum&0xFFFF)==0xFFFF){// store 4096*16 words(inverted lists) in one index file;
					System.out.println("next file");
					offset=0;
					fout.flush();//force to write out the buffer;
			        fout.close();
			        filename=wordTotalNum;// using words number as the name of index file gives more convenience for observation;
					fout = new BufferedOutputStream(new FileOutputStream(Integer.toString(filename)),128*1024);
				}
				//make new docIDsInList and freqsInList
				docIDsInList=new ArrayList<Integer>();
				freqsInList=new ArrayList<Byte>();
				compressedList = new ArrayList<byte[]>(4);
				for (int i=0;i<((word.length-1)>>>1);i++){
					docIDsInList.add(Integer.parseInt(word[i*2+1]));
					freqsInList.add((byte)Integer.parseInt(word[i*2+2]));
				}
			}else{// when the posting belonging to the same word, add them into doc id list and freq list;
				for (int i=0;i<((word.length-1)>>>1);i++){
					docIDsInList.add(Integer.parseInt(word[i*2+1]));
					freqsInList.add((byte)Integer.parseInt(word[i*2+2]));
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
		System.out.println(retValue);
	}

    
	public static void urlIndexWriter1(String filename){
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
//	            lexicon_string += word+" "+tf_offset.elementAt(0)+" "+tf_offset.elementAt(1);
	            lexicon_string = theWord+" "+lexinfo[0]+" "+lexinfo[1]+" "+lexinfo[2]+" "+lexinfo[3]+" "+lexinfo[4];
	            fout2.write(lexicon_string+"\n");
	        }
			fout2.flush();
			fout2.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}//end print url_index to disk
        System.out.println("done");
        //end loop
	}
	
	
	
	public static void urlIndexWriter(String filename){
	//start print url index to file
		System.out.println("start write url index into file");
		Iterator ilter1= index.urlDocMap.entrySet().iterator();
		try {
			//BufferedWriter fout2 = new BufferedWriter(new FileWriter("result/url_index.txt"));
			BufferedWriter fout2 = new BufferedWriter(new FileWriter(filename));
			fout2.write((--document_ID)+"\n");
			while (ilter1.hasNext())
	        {
				String url_string = new String();//store every line of url_index
				Map.Entry entry1 = (Map.Entry) ilter1.next();
	            int id = (int) entry1.getKey();
	            UrlDocLen url = (UrlDocLen) entry1.getValue();
				url_string += id+" "+url.url+" "+url.docLen; 
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
}
