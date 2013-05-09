from distutils import dir_util
import glob

data_dir = "/Users/jiankaidang/Documents/WebSearchEngines/backup_nyt_corpus/data/"
for directory in glob.glob(data_dir + "*/*"):
    try:
        dir_util.remove_tree(directory)
    except OSError as e:
        if e.errno == 20:
            continue