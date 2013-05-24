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
    result = []
    for item in processor.geoMap.items():
        try:
            if query.lower() in item[0].lower():
                result.append(item)
        except:
            continue
    if len(result) <= 100:
        result.sort(cmpMapsSearchResults, reverse=True)
    return JSONResponseMixin().render_to_response({
        "meta": result,
        "html": render_to_string('maps_results.html', {'foo': 'bar'})
    })


def cmpMapsSearchResults(result1, result2):
    return len(result1[1]["docs"]) - len(result2[1]["docs"])