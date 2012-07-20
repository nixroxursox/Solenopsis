import suds
import logging
import datetime
import solenopsis.config.cache
                         
class StandardAPI(object):  
    __ENTERPRISE_WSDL_PATH__ = 'file:///home/pcon/Dropbox/Development/salesforce/enterprise.min.xml'
    __ENTERPRISE_CLIENT__ = None

    def login(self, env):
        logging.getLogger('suds.client').setLevel(logging.CRITICAL)

        if self.__ENTERPRISE_CLIENT__ == None:
            logging.info('Generating Enterprise Client')
            self.__ENTERPRISE_CLIENT__ = suds.client.Client(self.__ENTERPRISE_WSDL_PATH__)

        if env.setDate != None and env.sessionLength != None:
            expireDate = env.setDate + datetime.timedelta(0, env.sessionLength)
            now = datetime.datetime.now()

            if expireDate > now:
                logging.info('We should have a valid session.  Skipping logging in')
                return
            else:
                logging.info('Our session has expired.')
        else:   
            logging.info('We do not have any info about the last time we were logged in')

        try:
            logging.info('Logging in')
            passToken = env.password + env.token
            loginResult = self.__ENTERPRISE_CLIENT__.service.login(env.username, passToken)
        except suds.WebFault as detail:
            raise Exception(detail)

        env.setSessionInfo(loginResult)
        env.writeConfig()