import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class WordMap {

	/* postingMap:
	 *  1.Map a word to all postings belonging to it; 
	 *  2. Map TermInDoc objects to a specific doc in the postings of the specific word;
	 *  3. There are term frequency stored for the specific document of the  specific word;
	 */
	public TreeMap<String, TreeMap<Integer, Integer>> postingMap;
	
	/*urlDocMap:
	 * 1. Map a document ID to a UrlDocLen object;
	 * 2. A UrlDocLen object stores the url and document length of the Doc ID;
	 */
	public Map<Integer, UrlDocLen> urlDocMap;
	
	/*lexiconMap:
	 * 1.Map a word to a 2 dimension array (extensible);
	 * 2.int[0] store the document frequency, and int[1] store the address of the indices(postings);
	 */
	public Map<String,int[]> lexiconMap;
	public double averageLen;	
	public int totalPageNum;
	
	WordMap(){
		//postingMap=new TreeMap<String,HashMap<Integer,TermInDoc>> ();
		urlDocMap=new HashMap<Integer, UrlDocLen> ();
		lexiconMap = new HashMap<String,int[]>();
		
	}
	
	
	/* insert a word into lexiconMap,  if it is existed, increment its document frequency;
	 * Note: increment the document frequency ONLY ONCE for each file;
	 */
	public void inSertIntoLexMap(String word,int docFreq,int filename,int startOffset,int len,int chunkNum){
//		int[] lexInfo=lexiconMap.get(word); //get the array of (time frequency + posting index) info of the give word;
//		if(lexInfo==null){  //if the lexicon is not existed
			int[] lexInfo=new int[5];
			lexInfo[0]=docFreq;  //document frequency;
			lexInfo[1]=filename; // index file number;
			lexInfo[2]=startOffset; //start point of the inverted list in the index file;
			lexInfo[3]=len;// the length of the inverted list by bytes;
			lexInfo[4]=chunkNum; //how many chunks for this inverted list;(The actual chunk number should be doubled because DocId chunks and frequency chunks are separated)
			lexiconMap.put(word,lexInfo);
//		}else{ //if existed, increment document frequency
//			lexInfo[0]++;
//			lexiconMap.put(word, lexInfo);
//		}
	}
	//overload inSertIntoLexMap
	public void inSertIntoLexMap(String word){
		lexiconMap.put(word,null);
	}
	
	/* insert the url and document length into the urlDocMap for the give doc ID;
	 */
	public void inSertIntoUrlDocMap(Integer docId,String url, Integer docLen){
		urlDocMap.put(docId,new UrlDocLen(url,docLen));
	}
	
	/* insert a posting into postingMap for the give word; 
	 * tempDocMap is a map from a docId of the give word to a TermInDoc object;
	 * TermInDoc object store all contexts and the term frequency of a given word in a give DocId
	 */
	public void inSertIntoPostingMap(String word,Integer docId){
		TreeMap<Integer, Integer> termFreqMap=postingMap.get(word); // find the termDocMap for the give map;	
		if(termFreqMap==null){    // the word is first time occurs;
			termFreqMap=new TreeMap<Integer,Integer>();
			termFreqMap.put(docId,1); // put the doc ID and context into termDocMap;
			postingMap.put(word, termFreqMap);
		}else{   // there is a mapping for this word;
			Integer freq=termFreqMap.get(docId);// the freq mapped from the given docId;
			if (freq==null){  // the Docment ID with the given word was not inserted before
				termFreqMap.put(docId,1); // put the doc ID into termFreqMap;
				postingMap.put(word, termFreqMap);
//				inSertIntoLexMap(word);// update the lexicon map by this word;
			}else{// document ID was existed in the map of the given word;
				freq+=1; // increment the term frequency of the given word in the docId;
//				if(freq>255) {freq=255;}
				termFreqMap.put(docId,freq);
				postingMap.put(word, termFreqMap);
			}
		}
	}
	
	/*set up lexicon map from the filewordmap
	 */
	public void setupLexicon(String lexicon_file) throws IOException
	{
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(lexicon_file));
			String line;
			while ((line = in.readLine()) != null) 
			{
				String word_lexinfo[] = line.split(" ");
				inSertIntoLexMap(word_lexinfo[0],Integer.parseInt(word_lexinfo[1]),Integer.parseInt(word_lexinfo[2]),
						Integer.parseInt(word_lexinfo[3]),Integer.parseInt(word_lexinfo[4]),Integer.parseInt(word_lexinfo[5]));
				//insert into lexicon
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{in.close();}
	}
	
	/*set up url map from the file
	 * */
	public void setupUrl(String url_file) throws IOException
	{
		long length = 0;
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(url_file));
			String line;
			totalPageNum = Integer.parseInt(in.readLine());
			while ((line = in.readLine()) != null) 
			{
				String words[] = line.split(" ");
				urlDocMap.put(Integer.parseInt(words[0]),new UrlDocLen(words[1],Integer.parseInt(words[2])));
				length += Integer.parseInt(words[2]);
			}
			//calculate the average length of documents in the collection
			averageLen = (double)length/urlDocMap.size();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally{in.close();}
		
	}
	
	public Byte convertStrToByte(String str){
		return (byte)str.charAt(0);		
	}
	
	public static int contextWeight(String context){
		switch(context){
			case "P":	return 1;
			case "T":	return 3;
			default: return 2;
		}
	}
} 


/*a class to store the url and document length of a given docId*/
class UrlDocLen{  
	String url;
	int docLen; //document length;
	public UrlDocLen(String url,int docLen){
		this.url=url;
		this.docLen=docLen;
	}
}


/* insert a word into lexiconMap,  if it is existed, increment its document frequency;
 * Note: increment the document frequency ONLY ONCE for each file;
 */
//public void inSertIntoLexMap(String word,int fileNum,int startOffset,int endOffset,int chunkNum){
//	int[] lexInfo=lexiconMap.get(word); //get the array of (time frequency + posting index) info of the give word;
//	if(lexInfo==null){  //if the lexicon is not existed
//		lexInfo=new int[4];
//		lexInfo[0]=1;  //document frequency;
//		lexInfo[1]=fileNum; // index file number;
//		lexInfo[2]=startOffset; //start point of the inverted list in the index file;
//		lexInfo[3]=endOffset;//end point of the inverted list in the index file;;
//		lexInfo[4]=chunkNum; //how many chunks for this inverted list;(The actual chunk number should be doubled because DocId chunks and frequency chunks are separated)
//		lexiconMap.put(word,lexInfo);
//	}else{ //if existed, increment document frequency
////		int temp = lexInfo.elementAt(0)+1;
////		lexInfo.setElementAt(temp, 0);
//		lexInfo[0]++;
//		lexiconMap.put(word, lexInfo);
//	}
//}

/*a class to store all contexts and the term frequency of a given word in a give DocId*/
//class TermInDoc{
//	int termFreq; //term frequency in a document;
//	List<Byte> contexts;  // all contexts of the given word in a document;
//	public TermInDoc(Byte context){
//		contexts=new LinkedList<Byte>();
//		contexts.add(context);
//		termFreq=1;
//	}
//}