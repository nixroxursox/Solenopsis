import os
import datetime
import logging
from configobj import ConfigObj

__ENVIRONMENT__ = None 
__CONFIG__ = None
__CONFIG_PROP__ = None
__DEPENDENT_ENV__ = None

__CURRENT_VERSION__ = '24.0'

PROP_FILE = 'solenopsis.properties'
CREDENTIALS_DIR = 'credentials'
CACHE_DIR = 'cache'

def gettime(dt_str):
    dt, _, us= dt_str.partition(".")
    dt= datetime.datetime.strptime(dt, "%Y-%m-%dT%H:%M:%S")
    us= int(us.rstrip("Z"), 10)
    return dt + datetime.timedelta(microseconds=us)

class Config(object):
    home = None
    master = None
    dependent = None
    local_home = None

    def __init__(self, *args, **kwargs):
        global __CONFIG_PROP__
        global __ENVIRONMENT__

        if kwargs.has_key('environment'):
            __ENVIRONMENT__ = kwargs.get('environment')

        if __CONFIG_PROP__ == None:
            self.home = os.path.expanduser('~')
            __CONFIG_PROP__ = os.path.join(self.home, PROP_FILE)

        if not os.path.isfile(__CONFIG_PROP__):
            raise Exception('Could not find primary config file')

        config = ConfigObj(__CONFIG_PROP__)

        self.home = os.path.expanduser(config['solenopsis.env.HOME'])
        self.master = config['solenopsis.env.MASTER']
        self.dependent = config['solenopsis.env.DEPENDENT']
        self.local_home = os.path.expanduser(config['solenopsis.env.local.HOME'])

        if __ENVIRONMENT__ == None:
            __ENVIRONMENT__ = self.dependent

        if not os.path.isdir(self.home):
            raise Exception('Could not find home dir')

class Environment(object):
    conf = None

    name = None
    username = None
    password = None
    token = None
    url = None

    sessionId = None
    serverUrl = None
    sessionLength = None
    setDate = None
    sandbox = False
    version = None

    def __init__(self, conf, envname):
        if self.conf == None:
            self.conf = __CONFIG__

        if envname == None:
            envname = self.conf.dependent

        self.name = envname

        config_file = os.path.join(self.conf.home, CREDENTIALS_DIR, self.name + '.properties')

        if not os.path.isfile(config_file):
            raise Exception('Could not load config file ['+config_file+']')

        config = ConfigObj(config_file)

        self.username = config['username'].decode('UTF-8')
        self.password = config['password'].decode('UTF-8')
        self.token = config['token'].decode('UTF-8')
        self.url = config['url'].decode('UTF-8')

        try:
            self.sessionId = config['sessionid'].decode('UTF-8')
        except KeyError as err:
            logging.debug('sessionid not found in config file')

        try:
            self.serverUrl = config['serverurl'].decode('UTF-8')
        except KeyError as err:
            logging.debug('serverurl not found in config file')

        try:
            self.sessionLength = int(config['sessionlength'])
        except KeyError as err:
            logging.debug('sessionlength not found in config file')

        try:
            self.setDate = gettime(config['setdate'])
        except KeyError as err:
            logging.debug('setdate not found in config file')

        try:
            self.sandbox = config['sandbox']
        except KeyError as err:
            logging.debug('sandbox not found in config file')

        try:
            self.version = config['version']
        except KeyError as err:
            logging.debug('version not found in config file')
            self.version = float(__CURRENT_VERSION__)


    def __str__(self):
        result = "Username: %s\nPassword: %s\nToken: %s\nURL: %s\n" % (self.username, self.password, self.token, self.url, )
        result += "Session Id: %s\nServer URL: %s\nSession Length: %s\nSet Date: %s\nSandbox: %s" % (self.sessionId, self.serverUrl, self.sessionLength, self.setDate, self.sandbox, )
        return result

    def writeConfig(self):
        config = ConfigObj()

        config['username'] = self.username
        config['password'] = self.password
        config['token'] = self.token
        config['url'] = self.url
        config['sessionid'] = self.sessionId
        config['serverurl'] = self.serverUrl
        config['sessionlength'] = self.sessionLength
        config['setdate'] = self.setDate.isoformat()+'Z'
        config['sandbox'] = self.sandbox
        config['version'] = self.version

        config_file = os.path.join(self.conf.home, CREDENTIALS_DIR, self.name + '.properties')
        cfile = open(config_file, 'w')

        config.write(cfile)

        cfile.close()

    def setSessionInfo(self, loginResult):
        self.sessionId = loginResult.sessionId
        self.serverUrl = loginResult.serverUrl
        self.sandbox = loginResult.sandbox
        self.sessionLength = loginResult.userInfo.sessionSecondsValid
        self.setDate = datetime.datetime.now()

def getEnv():
    global __ENVIRONMENT__
    return __ENVIRONMENT__

def setEnv(env):
    global __ENVIRONMENT__
    __ENVIRONMENT__ = env

def parseConfig():
    global __CONFIG__
    global __DEPENDENT_ENV__
    global __ENVIRONMENT__

    __CONFIG__ = Config()
    __DEPENDENT_ENV__ = Environment(None, __ENVIRONMENT__)