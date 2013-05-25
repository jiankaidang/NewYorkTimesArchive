from xml.etree.ElementTree import parse
from django.template import Context
from django.shortcuts import render
from django.template.loader import render_to_string

from NYTArchive.JSONResponseMixin import JSONResponseMixin
from query import processor


def geo(request):
    return JSONResponseMixin().render_to_response(processor.geoMap)


def search(request):
    context = {}
    return render(request, 'basic_search.html', context)


def query(request):
    if request.POST.has_key('search_box'):
        keyword = request.POST['search_box']
        result = processor.search_query(keyword)
        context = Context({
            'initial': False,
            'search_result': result,
            "keyword": keyword
        })
        return render(request, 'basic_search_result.html', context)
    else:
        context = Context({
            'initial': True,
        })
        return render(request, 'basic_search_result.html', context)


def search(request):
    context = {}
    return render(request, 'basic_search.html', context)


def time_line_query(request):
    if request.POST.has_key('search_box'):
        keyword = request.POST['search_box']
        start = request.POST['start']
        end = request.POST['end']
        result = processor.timeLine_query(keyword, start, end)
        context = Context({
            'initial': False,
            'search_result': result,
        })
        return render(request, 'timeLin_result.html', context)
    else:
        context = Context({
            'initial': True,
        })
        return render(request, 'timeLin_result.html', context)


def searchMaps(request):
    query = request.GET["query"]
    page = int(request.GET["page"])
    result = []
    for item in processor.geoMap.items():
        try:
            if query.lower() in item[0].lower():
                result.append(item)
        except:
            continue
    resultLen = len(result)
    html = ""
    if resultLen <= 50:
        result.sort(cmpMapsSearchResults, reverse=True)
        delta = 0
        if resultLen % 5:
            delta = 1
        totalPages = resultLen / 5 + delta
        result = result[(page - 1) * 5: page * 5]
        html = render_to_string('maps_results.html', {
            "result": result,
            "totalPages": totalPages,
            "range": range(1, totalPages + 1),
            "page": page
        })
    return JSONResponseMixin().render_to_response({
        "meta": result,
        "html": html
    })


def cmpMapsSearchResults(result1, result2):
    return len(result1[1]["docs"]) - len(result2[1]["docs"])


def searchLocation(request):
    location = request.GET["location"]
    articlePage = int(request.GET["articlePage"])
    result = []
    docs = processor.geoMap[location]["docs"]
    docs.sort(reverse=True)
    doc_meta = processor.doc_meta
    for docId in docs:
        # doc = doc_meta[int(docId)]
        # root = parse(
        #     "/Users/jiankaidang/Documents/WebSearchEngines/backup_nyt_corpus/data/all/" + doc.file_name + ".xml").getroot()
        root = parse(
            "/Users/jiankaidang/Documents/WebSearchEngines/backup_nyt_corpus/data/all/" + '%(docId)07d' % {
                "docId": int(docId)
            } + ".xml").getroot()
        result.append({
            # "url": doc.url,
            "url": root.findall('./head/pubdata')[0].get('ex-ref'),
            "hedline": root.findall('./body[1]/body.head/hedline/hl1')[0].text,
            "lead_paragraph": root.findall('./body/body.content/block[@class="full_text"]/p')[0].text
        })
    html = render_to_string('location_results.html', {
        "location": location,
        "len": len(docs),
        "result": result,
        # "totalPages": totalPages,
        # "range": range(1, totalPages + 1),
        # "page": page
    })
    return JSONResponseMixin().render_to_response(html)
