package org.solenopsis.tooling.main;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.LogManager;
import org.flossware.util.properties.impl.FilePropertiesMgr;
import org.solenopsis.lasius.credentials.Credentials;
import org.solenopsis.lasius.credentials.impl.PropertiesCredentials;
import org.solenopsis.lasius.sforce.wsimport.tooling.ApexClass;
import org.solenopsis.lasius.sforce.wsimport.tooling.ApexCodeCoverageAggregate;
import org.solenopsis.lasius.sforce.wsimport.tooling.ApexTrigger;
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
    private static final String SOQL_COVERAGE = "select NumLinesCovered, NumLinesUncovered, Coverage from ApexCodeCoverageAggregate where ApexClassOrTriggerId = ";
    private static final String SOQL_CLASSES = "select Id, Name from ApexClass";
    private static final String SOQL_TRIGGERS = "select Id, Name from ApexTrigger";

    private static final String SOQL_NEW_COVERAGE = "select Coverage from Code Coverage where ApexClassOrTrigger = ";

    private static Map<String, ApexClass> getApexClasses(final SforceServicePortType port) throws Exception {
        final Map<String, ApexClass> retVal = new HashMap<>();

        boolean isDone = false;
        QueryResult queryResult = port.query(SOQL_CLASSES);

        while (!isDone) {
            for(final SObject sobj : queryResult.getRecords()) {
                retVal.put(sobj.getId(), (ApexClass) sobj);
            }

            if (queryResult.isDone()) {
                isDone = true;
            } else {
                queryResult = port.queryMore(queryResult.getQueryLocator());
            }
        }

        return retVal;
    }

    private static Map<String, ApexTrigger> getApexTriggers(final SforceServicePortType port) throws Exception {
        final Map<String, ApexTrigger> retVal = new HashMap<>();

        QueryResult queryResult = port.query(SOQL_TRIGGERS);
        boolean isDone = false;

        while (!isDone) {
            for(final SObject sobj : queryResult.getRecords()) {
                retVal.put(sobj.getId(), (ApexTrigger) sobj);
            }

            if (queryResult.isDone()) {
                isDone = true;
            } else {
                queryResult = port.queryMore(queryResult.getQueryLocator());
            }
        }

        return retVal;
    }

//    private static double computePercentage(final ApexCodeCoverageAggregate coverage) {
//        if (coverage.getCoverage().getUncoveredLines().isEmpty()) {
//            return 0;
//        }
//
//        return ((double) coverage.getCoverage().getCoveredLines().size() / (coverage.getCoverage().getCoveredLines().size() + coverage.getCoverage().getUncoveredLines().size())) * 100;
//    }

    private static double computePercentage(final ApexCodeCoverageAggregate coverage) {
        return ((double) coverage.getNumLinesCovered() / (coverage.getNumLinesCovered() + coverage.getNumLinesUncovered())) * 100;
    }
    private static void emitTooling(final String wsdlType, Credentials creds) throws Exception {
        final SforceServicePortType port = SalesforceWebServiceUtil.createToolingPort(creds);

        final Map<String, ApexClass> classMap = getApexClasses(port);
        final Map<String, ApexTrigger> triggerMap = getApexTriggers(port);

        final TreeMap<String, String> classList = new TreeMap<>();
        final TreeMap<String, String> triggerList = new TreeMap<>();

        for (final String id : classMap.keySet()) {
            QueryResult queryResult = port.query(SOQL_COVERAGE + "'" + id + "'");

            for(final SObject sobj : queryResult.getRecords()) {
                ApexCodeCoverageAggregate coverage = (ApexCodeCoverageAggregate) sobj;


                classList.put(classMap.get(id).getName(),String.format("%-40s%13d%13d%13.2f", classMap.get(id).getName(), coverage.getNumLinesCovered(), coverage.getNumLinesUncovered(), computePercentage(coverage)));
            }
        }

        for (final String id : triggerMap.keySet()) {
            QueryResult queryResult = port.query(SOQL_COVERAGE + "'" + id + "'");

            for(final SObject sobj : queryResult.getRecords()) {
                ApexCodeCoverageAggregate coverage = (ApexCodeCoverageAggregate) sobj;


                triggerList.put(triggerMap.get(id).getName(),String.format("%-40s%13d%13d%13.2f", triggerMap.get(id).getName(), coverage.getNumLinesCovered(), coverage.getNumLinesUncovered(), computePercentage(coverage)));
            }
        }

        System.out.println("\n\n");
        System.out.println("Class Coverage:");
        System.out.printf("    %40s%13s%13s%15s", " ", "Covered", "Uncovered", "Percent\n");

        for (final String str : classList.values()) {
            System.out.println("    " + str + "%");
        }

        System.out.println("\n\n");
        System.out.println("Trigger Coverage:");
        System.out.printf("    %40s%13s%13s%15s", " ", "Covered", "Uncovered", "Percent\n");

        for (final String str : triggerList.values()) {
            System.out.println("    " + str + "%");
        }
    }

    public static void main(final String[] args) throws Exception {
        LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));

        final String env = "test-dev.properties";
//        final String env = "qa.properties";
//        final String env = "dev.properties";

        emitTooling("Partner WSDL", new PropertiesCredentials(new FilePropertiesMgr(System.getProperty("user.home") + "/.solenopsis/credentials/" + env)));

    }
}
