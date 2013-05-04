from django.template import Context
from NYTArchive.JSONResponseMixin import JSONResponseMixin
from query import processor
from django.shortcuts import render


def geo(request):
    return JSONResponseMixin().render_to_response(processor.geoMap.keys())

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