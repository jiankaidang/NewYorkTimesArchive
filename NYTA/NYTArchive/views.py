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
    keyword = request.POST['search_box']
    result = processor.search_query(keyword)
    context = Context({
        'search_result': result,
    })
    return render(request, 'basic_search_result.html', context)


def search(request):
    context = {}
    return render(request, 'basic_search.html', context)


def query(request):
    keyword = request.POST['search_box']
    result = processor.search_query(keyword)
    context = Context({
        'search_result': result,
    })
    return render(request, 'basic_search_result.html', context)


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