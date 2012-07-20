import solenopsis.config
from solenopsis.ws.standard import StandardAPI
from solenopsis.ws.metadata import MetadataAPI
import logging

logging.basicConfig(format='%(levelname)s: %(message)s', level=logging.INFO)

solenopsis.config.parseConfig()

standard = StandardAPI()
standard.login(solenopsis.config.__DEPENDENT_ENV__)

#metadata = MetadataAPI()
#metadata.describeMetadata(standard, solenopsis.config.__DEPENDENT_ENV__)

#print metadata.suffixMap.keys()