import suds              
import logging           
import datetime
import solenopsis.config.cache

class Metadata(object):
    childXmlNames = []
    directoryName = None
    inFolder = False
    metaFile = False
    suffix = None
    xmlName = None

    def __str__(self):
        result = "Child XML Names: %s\nDirectory Name: %s\nIn Folder: %s\nMeta File: %s\nSuffix: %s\nXML Name: %s\n" % (self.childXmlNames, self.directoryName, self.inFolder, self.metaFile, self.suffix, self.xmlName,)
        return result
                         
class MetadataAPI(object):  
    __METADATA_WSDL_PATH__ = 'file:///home/pcon/Dropbox/Development/salesforce/metadata.xml'
    __METADATA_CLIENT__ = None

    metadataInfo = None
    suffixMap = None

    def buildSuffixMap(self):
        if self.suffixMap == None:
            self.suffixMap = {}

        for meta in self.metadataInfo:
            self.suffixMap[meta.suffix] = meta


    def describeMetadata(self, standard, env):
        logging.getLogger('suds.client').setLevel(logging.CRITICAL)

        cache_name = env.name + '.metadata'

        if solenopsis.config.cache.hasCache(env, cache_name):
            logging.info('Fetching metadata from cache')
            self.metadataInfo = solenopsis.config.cache.readCache(env, cache_name)
            self.buildSuffixMap()
            return

        if self.__METADATA_CLIENT__ == None:
            sessionHeader = standard.__ENTERPRISE_CLIENT__.factory.create('SessionHeader')
            sessionHeader.sessionId = env.sessionId

            self.__METADATA_CLIENT__ = suds.client.Client(self.__METADATA_WSDL_PATH__)
            self.__METADATA_CLIENT__.set_options(soapheaders=sessionHeader)

        try:
            logging.info('Describing Metadata')
            describeResult = self.__METADATA_CLIENT__.service.describeMetadata(env.version)
        except suds.WebFault as detail:
            raise Exception(detail)

        if self.metadataInfo == None:
            self.metadataInfo = []

        for metadataObject in describeResult.metadataObjects:
            meta = Metadata()

            if hasattr(metadataObject, 'directoryName'):
                meta.directoryName = metadataObject.directoryName

            if hasattr(metadataObject, 'inFolder'):
                meta.inFolder = metadataObject.inFolder

            if hasattr(metadataObject, 'metaFile'):
                meta.metaFile = metadataObject.metaFile

            if hasattr(metadataObject, 'suffix'):
                meta.suffix = metadataObject.suffix

            if hasattr(metadataObject, 'xmlName'):
                meta.xmlName = metadataObject.xmlName

            if hasattr(metadataObject, 'childXmlNames'):
                meta.childXmlNames = metadataObject.childXmlNames

            self.metadataInfo.append(meta)

        solenopsis.config.cache.writeCache(env, cache_name, self.metadataInfo)

        buildSuffixMap()