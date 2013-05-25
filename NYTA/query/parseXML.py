__author__ = 'linbinbin'
import xml.etree.ElementTree as ET

def parse_title(path):
    #tree = ET.parse('C:\\ubuntu_share\\workspace\\ExtraFile\\data\\all\\0000000.xml')
    tree = ET.parse(path)
    root = tree.getroot()
    title_str = []
    for title in root.iter('title'):
        title_str = title.text
    return title_str
def parse_content(path):
    content_str = ''
    tree = ET.parse(path)
    root = tree.getroot()
    for body in root.findall('./body/body.content/block[@class="full_text"]'):
        for content in body.findall('p'):
            tmp = content.text
            content_str +=tmp
    return content_str
def parse_time(path):
    time = ''
    tree = ET.parse(path)
    root = tree.getroot()
    for attr in root.iter('pubdata'):
        time = attr.get("date.publication")
    return time