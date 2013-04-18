from distutils import dir_util
import glob
import tarfile

data_dir = "D:\Note_for_Class\2013spring\web-search\project\backup_nyt_corpus\data"
for directory in glob.glob(data_dir + "*"):
    for tgzFile in glob.glob(directory + "/*"):
        tar = tarfile.open(tgzFile)
        tar.extractall(directory)
        tar.close()
for directory in glob.glob(data_dir + "*/*/*"):
    dir_util.copy_tree(directory, data_dir + "all")