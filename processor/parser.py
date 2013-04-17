__author__ = 'Jiankai Dang'
from nltk.tokenize import wordpunct_tokenize


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
            res.append(term)
    return res


if __name__ == "__main__":
    while True:
        input = raw_input(">")
        print parse(input)