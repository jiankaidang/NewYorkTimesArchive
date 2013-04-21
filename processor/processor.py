__author__ = 'Jiankai Dang'
from heapq import heappush, heappop
from math import log
import random
from time import time

from nltk.tokenize import wordpunct_tokenize

from encode import decode7bit


################## Initialize Lexicon and Doc meta data part######################
class lexicon_node:
#    lexicon class
    def __init__(self):
        self.start = -1 # line number in lexicon file
        self.total = -1
        self.did = -1
        self.length = -1
        self.meta_length = -1
        #        self.number = -1

    def display(self):
        print self.file_name, str(self.total), str(self.start), str(self.length)


class doc_node:
#    doc meta class
    def __init__(self):
        self.url = -1
        self.id = -1
        self.total = -1
        self.pr = -1
        self.ar = -1
        # self.number = -1

    def display(self):
        print self.url, str(self.id), str(self.total), str(self.pr)


def parse(query):
    # this function parse input query into a split word
    # e.g. "it's" into "it ' s"
    # also delete uesless terms
    try:
        query = wordpunct_tokenize(query)
    except Exception:
        query = query.split()
    print query
    res = []
    for term in query:
        flag = True
        for digit in term:
            if not (digit.isalpha() or digit.isdigit()):
                flag = False
        if flag:
            res.append(term.lower())
    return res

# hard code here
lexicon_file_line_number = 0


def build_lexicon(path):
#    read in lexicon file into memory
    global lexicon_list
    global word_list
    global d_avg
    global lexicon_file_line_number
    for line in open(path):
        w = line.split()
        if len(w) == 8:
            lexicon_obj = lexicon_node()
            id = int(w[1])
            word_list[w[0].lower()] = id
            lexicon_obj.start = w[4]
            lexicon_obj.total = w[3]
            lexicon_obj.did = w[2]
            lexicon_obj.meta_length = w[6]
            lexicon_obj.length = w[7]
            lexicon_list.append(lexicon_obj)
            d_avg += float(w[5])
            lexicon_file_line_number = lexicon_file_line_number + 1
    d_avg /= float(lexicon_file_line_number)
    return


def build_doc_meta_data(path):
#    read in doc meta data file into memory
    global doc_list
    global doc_meta
    is_init = False
    for line in open(path):
        w = line.split()
        if len(w) == 1 and is_init == False:
            for i in range(0, int(w[0])):
                doc_meta.append(doc_node())
        else:
            id = int(w[0])
            doc_list[w[1]] = id
            doc_meta[id].url = w[1]
            doc_meta[id].total = w[2]
            #            doc_meta[id].pr = w[3]
            #            print id
            #            print doc_meta[id].url
            #            sleep(1)
            #            print w[1]
            #            doc_meta[id].pr = float(getPageRank(w[1]))
            #            doc_meta[id].ar = float(getAlexaRank(w[1]))
    return

################## Initialize Lexicon and Doc meta data part######################

################## Basic Search APIs ######################

#DaaT functions begin
# Open the inverted list for a specific term.
# @param termId {integer} Term id
# @param getCache {Boolean} Whether to get the inverted list information from cache. Default value is False.
# @return {Dictionary} A dictionary of inverted list information.
def openList(termId, getCache=False):
    if getCache:
        if is_cached(termId):
            data = get_cache_data(termId)
            # Reset the index information.
            data["current_chunk_index"] = 0
            data["current_posting_index"] = 0
            return data
    lexicon_node_obj = lexicon_list[termId]
    # Open to read the inverted list file.
    list_file = open(pwd + str(lexicon_node_obj.did), "rb")
    # Seek to the start offset of the inverted list information for this term.
    list_file.seek(int(lexicon_node_obj.start))
    list_data_str = list_file.read(int(lexicon_node_obj.length))
    list_posting = {
        "current_chunk_index": 0,
        "current_posting_index": 0,
        "current_freq": 0,
        "meta_data": [],
        # The inverted list file name.
        "did": lexicon_node_obj.did
    }
    #    print "lexicon_node_obj.start:" + str(lexicon_node_obj.start)
    #    print "lexicon_node_obj.len:" + str(lexicon_node_obj.length)
    # Decode the meta data information.
    list_data = decode7bit(list_data_str[:int(lexicon_node_obj.meta_length)])
    list_file.close()
    # print "len(list_data):---" + str(len(list_data))
    for i in range(0, len(list_data), 2):
        if i != 0:
            # Decode the document id information.
            list_data[i] += list_data[i - 2]
        list_posting["meta_data"].append({
            # The last document id of this chunk.
            "did": list_data[i],
            "chunk_size": list_data[i + 1]
        })

    if True:
    # if getCache:
        # Store the string of chunks data into memory.
        list_posting["chunks_str"] = list_data_str[int(lexicon_node_obj.meta_length):]
    else:
        size = 0
        chunks_str = list_data_str[int(lexicon_node_obj.meta_length):]
        for i in range(0, len(list_data), 2):
            chunk_content = decode7bit(chunks_str[size:size + list_data[i + 1]])
            chunk_postings = []
            for j in range(0, len(chunk_content), 2):
                if j != 0:
                    chunk_content[j] += chunk_content[j - 2]
                elif i != 0:
                    chunk_content[j] += list_data[i - 2]
                chunk_postings.append({
                    "did": chunk_content[j],
                    "freq": chunk_content[j + 1]
                })
            list_posting[i / 2] = chunk_postings
            size += list_data[i + 1]
    return list_posting


def closeList(term):
    return

# Find the next document id greater than or equal to a specific document id.
# @param list_posting {Dictionary} The inverted list information dictionary.
# @param k_docID {Integer} The document id to compare to.
# @return {Integer} The next document id greater than or equal to a specific document id or return max_doc_id if not any.
def nextGEQ(list_posting, k_docID):
    current_chunk_index = int(list_posting["current_chunk_index"])
    meta_data = list_posting["meta_data"]
    current_posting_index = int(list_posting["current_posting_index"])
    meta_data_length = len(meta_data)
    while current_chunk_index < meta_data_length:
        # The last document id of current chunk.
        did = meta_data[current_chunk_index]["did"]
        if did >= k_docID:
            break
        current_chunk_index += 1
        current_posting_index = 0
    if current_chunk_index >= meta_data_length:
        return max_doc_id
    if current_chunk_index in list_posting:
        for j in range(current_posting_index, len(list_posting[current_chunk_index])):
            next_did = list_posting[current_chunk_index][j]["did"]
            if next_did >= k_docID:
                list_posting["current_posting_index"] = j
                list_posting["current_freq"] = list_posting[current_chunk_index][j]["freq"]
                return next_did
    else:
        size = 0
        for meta_index in range(current_chunk_index):
            # Calculate the offset of the chunk.
            size += int(list_posting["meta_data"][meta_index]["chunk_size"])
            # Decode the chunk content.
        chunk_content = decode7bit(
            list_posting["chunks_str"][size:meta_data[current_chunk_index]["chunk_size"]])
        chunk_postings = []
        next_did = -1
        for i in range(0, len(chunk_content), 2):
            if i != 0:
                # Decode the document id.
                chunk_content[i] += chunk_content[i - 2]
            elif current_chunk_index != 0:
                chunk_content[i] += meta_data[current_chunk_index - 1]["did"]
            chunk_postings.append({
                "did": chunk_content[i],
                "freq": chunk_content[i + 1]
            })
            if chunk_content[i] >= k_docID and next_did == -1:
                list_posting["current_posting_index"] = i / 2
                list_posting["current_freq"] = chunk_content[i + 1]
                next_did = chunk_content[i]
        list_posting[current_chunk_index] = chunk_postings
        if next_did != -1:
            list_posting["current_chunk_index"] = current_chunk_index
            return next_did
    return max_doc_id


def getFreq(list_posting):
    return list_posting["current_freq"]

#DaaT functions end
################## Basic Search APIs ######################

################## Search APIs######################

def compute_BM25(terms, did, freq):
#    function to calculate BM25 score
    global max_doc_id
    global d_avg
    res = 0.0
    if did < 0 or did >= max_doc_id or len(terms) == 0 or len(freq) == 0:
        return -1.0
    if len(terms) != len(freq):
        print "error: len(terms) != len(freq)\n"
    n = 2635851.0
    d = float(doc_meta[did].total)
    k1 = 1.2
    b = 0.75
    K = k1 * (1 - b + b * d / d_avg)
    for i in range(0, len(terms)):
        fdt = float(freq[i])
        ft = float(lexicon_list[word_list[terms[i]]].total) / fdt
        if n < ft:
            ft = n
        res += log((n - ft + 0.5) / (ft + 0.5)) * (k1 + 1.0) * fdt / (K + fdt)
    return res


def compute_score(terms, did, freq):
#    compute score based on BM25,
    BM25 = compute_BM25(terms, did, freq)
    k1 = 1.0
    try:
        PageRank = doc_meta[did].pr
        AlexRank = doc_meta[did].ar
    except Exception:
        print "did out of range"
        print did
        PageRank = -1
        AlexRank = -1
    res = BM25
    if PageRank < 0:
        res *= k1 * 1.0
    else:
        res *= k1 * PageRank
    if AlexRank < 0:
        res *= 1.0
    else:
        res *= (1.0 + 1.0 / AlexRank)
    return res


def search_query(query):
    global max_doc_id
    global top
    res = []
    # query = query.split()
    #    print query
    query = parse(query)
    qq = []
    for qt in query:
        if qt in word_list:
            qq.append(qt)
    query = qq
    #    print "Query are: "
    #    print query
    if len(query) == 0:
        return res
    ip = []
    d = []
    for q in query:
    #        ip.append(word_list[q])
        ip.append(openList(word_list[q], True))
        #    ip = openList(ip)??? openList one term??
    #    print "ip are: "
    #    print ip

    if len(ip) == 0:
        return res

    res_q = [] # heap of #top results
    num = len(ip)
    did = 0

    while (did < max_doc_id):
    #        print did
        # get next post from shortest list
        did = nextGEQ(ip[0], did)

        # see if you find entries with same docID in other lists
        # for (i=1; (i<num) && ((d=nextGEQ(lp[i], did)) == did); i++);
        d = -1
        for i in range(1, num):
            d = nextGEQ(ip[i], did)
            if d != did:
                break
                #            print i
                #            print d
                # not in intersection
        if d > did:
            did = d
        else:
            # docID is in intersection; now get all frequencies
            # for (i=0; i<num; i++)  f[i] = getFreq(lp[i], did);
            f = []
            for i in range(0, num):
                f.append(getFreq(ip[i]))
                #            print "get one page, id: "
            #            print did
            # compute BM25 score from frequencies and other data
            temp = compute_score(query, did, f)
            #            print "score: "
            #            print temp
            if did >= max_doc_id:
                break
            if len(res_q) < top:
                heappush(res_q, (temp, did))
            elif res_q[0][0] < temp:
                heappop(res_q)
                heappush(res_q, (temp, did))
                # to do top10, using priority queue
            #            print "DID:!!!!"
            #            print did
            # and increase did to search for next post
            did = did + 1

            #    for i in range(0, num):
            #        closeList(ip[i])
            #    print res_q
    res_q = sorted(res_q, key=lambda tup: tup[0])
    #    print res_q
    for i in reversed(range(0, len(res_q))):
        url = doc_meta[res_q[i][1]].url
        #        print res_q[i][0]
        #        print res_q[i][1]
        #        print url
        res.append((res_q[i][0], url, res_q[i][1]))
        #    print res

    display_simple_result(res)
    return res

################## Search APIs######################

################## Display APIs######################

def display_simple_result(result_set):
    print "There are " + str(len(result_set)) + " results.\n Simple Result:\n"
    for i in range(0, len(result_set)):
        print "Result #" + str(i)
        r = result_set[i]
        print r[0], r[1], r[2]
    return

################## Display APIs######################


################## Cache APIs######################

def make_decision_and_do_cache(cache_num=500000, path="frequency.txt"):
#    This function selects terms to do cache
#    This function read a bag of words with frequency in common English. In decending order of this frequency, do cache.
    cached_num = 0
    for line in open(path):
    #        print "cached num:"
    #        print cached_num
        line = line.split()
        if len(line) == 3:
            word = line[0].lower()
            freq = int(line[1])
            if do_cache(word):
                cached_num += 1
            if cached_num == cache_num:
                break
        else:
            pass
    if cached_num < cache_num:
        for i in range(cache_num, cache_num):
            do_cache("")


def do_cache(word):
#    this function fo cache of selected word
#    if no word as input, do cache of a random word not cached
#    return true if cache successfully
#    return false if not
    #if word == "", do random
    if word != "":
        if word in word_list:
            # do cache
            cached_data[word_list[word]] = openList(word_list[word]) # to do waiting for Jiankai's API
        else:
            # could not cache
            return False
    else:
        while True:
            t = random.uniform(0, lexicon_file_line_number - 1)
            if not is_cached(t):
                cached_data[t] = openList(word_list[word])
                break
    return True


def is_cached(word_id):
#    check if the word with this word id is cached
    return word_id in cached_data


def get_cache_data(word_id):
#    given a word_id, return the cached data
    if word_id in cached_data:
        return cached_data[word_id]

################## Cache APIs######################


################## Main Function######################
# basic variables initialization here
top = 10
d_avg = 0.0
#main function
doc_list = {}
doc_meta = []
lexicon_list = []
word_list = {}

pwd = "/Users/jiankaidang/Documents/WebSearchEngines/NYTAData/"
print "Building Doc Meta Data...\n"
build_doc_meta_data(pwd + "result/url_index.txt")
print "Building Lexicon Meta Data..."
build_lexicon(pwd + "result/lexicon_index.txt")
result_set = []
max_doc_id = len(doc_list)
cached_data = {}
print "Caching...\n"
make_decision_and_do_cache()
print "Cache done\n"

while (True):
    input = raw_input("> input query: search or quit\n")
    if (input == "quit"):
        break
    if input == "search":
        query = raw_input("your query: ")
        _time = time()
        result_set = search_query(query)
        _time = time() - _time
        print "time: ", str(_time)
    #        try:
    #            result_set = search_query(query)
    #        except Exception:
    #            print Exception
    else:
        print "error: invalid command"
        ################## Main Function######################