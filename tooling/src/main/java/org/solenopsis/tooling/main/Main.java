package org.solenopsis.tooling.main;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.LogManager;
import org.flossware.util.properties.impl.FilePropertiesMgr;
import org.solenopsis.lasius.credentials.Credentials;
import org.solenopsis.lasius.credentials.impl.PropertiesCredentials;
import org.solenopsis.lasius.sforce.wsimport.tooling.ApexClass;
import org.solenopsis.lasius.sforce.wsimport.tooling.ApexCodeCoverage;
import org.solenopsis.lasius.sforce.wsimport.tooling.ApexTrigger;
import org.solenopsis.lasius.sforce.wsimport.tooling.Coverage;
import org.solenopsis.lasius.sforce.wsimport.tooling.QueryResult;
import org.solenopsis.lasius.sforce.wsimport.tooling.SObject;
import org.solenopsis.lasius.sforce.wsimport.tooling.SforceServicePortType;
import org.solenopsis.lasius.wsimport.util.SalesforceWebServiceUtil;


/**
 *
 * The purpose of this class is emit tooling to the console.
 *
 * @author sfloess
 *
 */
public class Main {
    private static final String SOQL_COVERAGE = "select ApexClassorTriggerId, NumLinesCovered, NumLinesUncovered, Coverage from ApexCodeCoverage";
    private static final String SOQL_CLASSES = "select Id, Name from ApexClass";
    private static final String SOQL_TRIGGERS = "select Id, Name from ApexTrigger";

    private static Map<String, ApexClass> getApexClasses(final SforceServicePortType port) throws Exception {
        final Map<String, ApexClass> retVal = new HashMap<>();

        final QueryResult queryResult = port.query(SOQL_CLASSES);
        for(final SObject sobj : queryResult.getRecords()) {
            retVal.put(sobj.getId(), (ApexClass) sobj);
        }

        return retVal;
    }

    private static Map<String, ApexTrigger> getApexTriggers(final SforceServicePortType port) throws Exception {
        final Map<String, ApexTrigger> retVal = new HashMap<>();

        final QueryResult queryResult = port.query(SOQL_TRIGGERS);
        for(final SObject sobj : queryResult.getRecords()) {
            retVal.put(sobj.getId(), (ApexTrigger) sobj);
        }

        return retVal;
    }

    private static double computePercentage(final ApexCodeCoverage coverage) {
        return ((double) coverage.getCoverage().getCoveredLines().size() / (coverage.getCoverage().getCoveredLines().size() + coverage.getCoverage().getUncoveredLines().size())) * 100;
    }

    private static void emitTooling(final String wsdlType, Credentials creds) throws Exception {
        final SforceServicePortType port = SalesforceWebServiceUtil.createToolingPort(creds);

        final Map<String, ApexClass> classMap = getApexClasses(port);
        final Map<String, ApexTrigger> triggerMap = getApexTriggers(port);

        final Map<String, ApexCodeCoverage> classList = new TreeMap<>();
        final Map<String, ApexCodeCoverage> triggerList = new TreeMap<>();

        final QueryResult queryResult = port.query(SOQL_COVERAGE);

        for(final SObject sobj : queryResult.getRecords()) {
            ApexCodeCoverage coverage = (ApexCodeCoverage) sobj;

            if (classMap.containsKey(coverage.getApexClassOrTriggerId())) {

                classList.put(classMap.get(coverage.getApexClassOrTriggerId()).getName(), coverage);
            } else if (triggerMap.containsKey(coverage.getApexClassOrTriggerId())) {
                triggerList.put(triggerMap.get(coverage.getApexClassOrTriggerId()).getName(), coverage);
            }
        }

        System.out.println("Triggers:");

        for (final String str : triggerList.keySet()) {
            System.out.printf("    %-40s%13.2f", str, computePercentage(triggerList.get(str)));
            System.out.println("%");
        }

        System.out.println("\nClasses:");

        for (final String str : classList.keySet()) {
            System.out.printf("    %-40s%13.2f", str, computePercentage(classList.get(str)));
            System.out.println("%");
        }
    }

    public static void main(final String[] args) throws Exception {
        LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));

        final String env = "test-dev.properties";
//        final String env = "dev.properties";

        emitTooling("Partner WSDL", new PropertiesCredentials(new FilePropertiesMgr(System.getProperty("user.home") + "/.solenopsis/credentials/" + env)));

        //emitTooling("Partner WSDL", credentials, new PartnerSecurityMgr());
        //emitTooling("Enterprise WSDL", credentials, new EnterpriseSecurityMgr());
    }
}
