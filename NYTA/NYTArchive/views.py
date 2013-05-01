from NYTArchive.JSONResponseMixin import JSONResponseMixin
from query import processor


def geo(request):
    return JSONResponseMixin().render_to_response(processor.geoMap.keys())