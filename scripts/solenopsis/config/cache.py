import os
import pickle
import solenopsis.config

def getPath(env, cache_name):
    return os.path.join(env.conf.home, solenopsis.config.CACHE_DIR, cache_name)

def hasCache(env, cache_name):
    return os.path.isfile(getPath(env, cache_name))

def readCache(env, cache_name):
    cache_file = open(getPath(env, cache_name), 'rb')
    return pickle.load(cache_file)

def writeCache(env, cache_name, obj):
    cache_dirpath = os.path.join(env.conf.home, solenopsis.config.CACHE_DIR)

    if not os.path.isdir(cache_dirpath):
        os.mkdir(cache_dirpath)

    cache_file = open(getPath(env, cache_name), 'wb')
    pickle.dump(obj, cache_file, -1)

def clearCache(env, cache_name):
    if hasCache(env, cache_name):
        os.remove(getPath(env, cache_name))