from django.conf.urls import patterns, url

# Uncomment the next two lines to enable the admin:
# from django.contrib import admin
# admin.autodiscover()
from NYTArchive import views

urlpatterns = patterns('',
                       # Examples:
                       # url(r'^$', 'NYTA.views.home', name='home'),
                       # url(r'^NYTA/', include('NYTA.foo.urls')),

                       # Uncomment the admin/doc line below to enable admin documentation:
                       # url(r'^admin/doc/', include('django.contrib.admindocs.urls')),

                       # Uncomment the next line to enable the admin:
                       # url(r'^admin/', include(admin.site.urls)),

                       url(r'^$', views.index, name='index')
)
