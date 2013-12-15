package org.solenopsis.tooling.main;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.LogManager;
import org.flossware.util.properties.impl.FilePropertiesMgr;
import org.solenopsis.lasius.credentials.Credentials;
import org.solenopsis.lasius.credentials.impl.PropertiesCredentials;
import org.solenopsis.lasius.wsimport.session.mgr.impl.SingleSessionMgr;
import org.solenopsis.wsdls.tooling.ApexClass;
import org.solenopsis.wsdls.tooling.ApexCodeCoverageAggregate;
import org.solenopsis.wsdls.tooling.ApexTrigger;
import org.solenopsis.wsdls.tooling.QueryResult;
import org.solenopsis.wsdls.tooling.SObject;
import org.solenopsis.wsdls.tooling.SforceServicePortType;
import org.solenopsis.wsdls.tooling.SforceServiceService;
import org.solenopsis.lasius.wsimport.util.SalesforceWebServiceUtil;


/**
 *
 * The purpose of this class is emit tooling to the console.
 *
 * @author sfloess
 *
 */
public class Main {
//
//    private static Map<String, ApexClass> getApexClasses(final SforceServicePortType port) throws Exception {
//        final Map<String, ApexClass> retVal = new HashMap<>();
//
//        boolean isDone = false;
//        QueryResult queryResult = port.query(SOQL_CLASSES);
//
//        while (!isDone) {
//            for(final SObject sobj : queryResult.getRecords()) {
//                retVal.put(sobj.getId(), (ApexClass) sobj);
//            }
//
//            if (queryResult.isDone()) {
//                isDone = true;
//            } else {
//                queryResult = port.queryMore(queryResult.getQueryLocator());
//            }
//        }
//
//        return retVal;
//    }
//
//    private static Map<String, ApexTrigger> getApexTriggers(final SforceServicePortType port) throws Exception {
//        final Map<String, ApexTrigger> retVal = new HashMap<>();
//
//        QueryResult queryResult = port.query(SOQL_TRIGGERS);
//        boolean isDone = false;
//
//        while (!isDone) {
//            for(final SObject sobj : queryResult.getRecords()) {
//                retVal.put(sobj.getId(), (ApexTrigger) sobj);
//            }
//
//            if (queryResult.isDone()) {
//                isDone = true;
//            } else {
//                queryResult = port.queryMore(queryResult.getQueryLocator());
//            }
//        }
//
//        return retVal;
//    }
//
//    private static Map<String, ApexCodeCoverageAggregate> getApexCodeCoverage(final SforceServicePortType port) throws Exception {
//        final Map<String, ApexCodeCoverageAggregate> retVal = new HashMap<>();
//
//        QueryResult queryResult = port.query(SOQL_COVERAGE);
//        boolean isDone = false;
//
//        while (!isDone) {
//            for(final SObject sobj : queryResult.getRecords()) {
//                retVal.put(sobj.getId(), (ApexCodeCoverageAggregate) sobj);
//            }
//
//            if (queryResult.isDone()) {
//                isDone = true;
//            } else {
//                queryResult = port.queryMore(queryResult.getQueryLocator());
//            }
//        }
//
//        return retVal;
//    }

//    private static double computePercentage(final ApexCodeCoverageAggregate coverage) {
//        if (coverage.getCoverage().getUncoveredLines().isEmpty()) {
//            return 0;
//        }
//
//        return ((double) coverage.getCoverage().getCoveredLines().size() / (coverage.getCoverage().getCoveredLines().size() + coverage.getCoverage().getUncoveredLines().size())) * 100;
//    }

    private static final String SOQL_COVERAGE = "select ApexClassorTriggerId, Id, NumLinesCovered, NumLinesUncovered, Coverage from ApexCodeCoverageAggregate ";
    private static final String SOQL_CLASSES = "select Id, Name from ApexClass";
    private static final String SOQL_TRIGGERS = "select Id, Name from ApexTrigger";

    private static <S> Map<String, S> getData(final SforceServicePortType port, final String soql) throws Exception {
        final Map<String, S> retVal = new HashMap<>();

        QueryResult queryResult = port.query(soql);
        boolean isDone = false;

        while (!isDone) {
            for(final SObject sobj : queryResult.getRecords()) {
                retVal.put(sobj.getId(), (S) sobj);
            }

            if (queryResult.isDone()) {
                isDone = true;
            } else {
                queryResult = port.queryMore(queryResult.getQueryLocator());
            }
        }

        return retVal;
    }

    private static double computePercentage(final ApexCodeCoverageAggregate codeCoverage) {
        if (codeCoverage.getCoverage().getUncoveredLines().isEmpty()) {
            return 0;
        }

//        return ((double) coverage.getNumLinesCovered() / (coverage.getNumLinesCovered() + coverage.getNumLinesUncovered())) * 100;
        return ((double) codeCoverage.getCoverage().getCoveredLines().size() / (codeCoverage.getCoverage().getCoveredLines().size() + codeCoverage.getCoverage().getUncoveredLines().size())) * 100;
    }

    private static String computeLine(final String name, final ApexCodeCoverageAggregate codeCoverage) {
        if (null == codeCoverage) {
            return String.format("%-40s%13d%13d%13.2f", name, 0, 0, 0.0);
        }

        return String.format("%-40s%13d%13d%13.2f", name, codeCoverage.getCoverage().getCoveredLines().size(), codeCoverage.getCoverage().getUncoveredLines().size(), computePercentage(codeCoverage));
    }

    private static Map<String, ApexCodeCoverageAggregate> getCodeCoverage(final SforceServicePortType port) throws Exception {
        final Map<String, ApexCodeCoverageAggregate> coverageMap = getData(port, SOQL_COVERAGE);
        final Map<String, ApexCodeCoverageAggregate> retVal = new HashMap<>();

        for (ApexCodeCoverageAggregate coverage : coverageMap.values()) {
            retVal.put(coverage.getApexClassOrTriggerId(), coverage);
        }

        return retVal;
    }

    private static Map<String, String> getClassCoverage(final SforceServicePortType port, final Map<String, ApexCodeCoverageAggregate> coverageMap) throws Exception {
        final Map<String, ApexClass> map    = getData(port, SOQL_CLASSES);
        final Map<String, String>    retVal = new TreeMap<>();

        for (final ApexClass apexClass : map.values()) {
            retVal.put(apexClass.getName(), computeLine(apexClass.getName(), coverageMap.get(apexClass.getId())));
        }



        return retVal;
    }

    private static Map<String, String> getTriggerCoverage(final SforceServicePortType port, final Map<String, ApexCodeCoverageAggregate> coverageMap) throws Exception {
        final Map<String, ApexTrigger> map    = getData(port, SOQL_TRIGGERS);
        final Map<String, String>      retVal = new TreeMap<>();

        for (final ApexTrigger apexTrigger : map.values()) {
            retVal.put(apexTrigger.getName(), computeLine(apexTrigger.getName(), coverageMap.get(apexTrigger.getId())));
        }

        return retVal;
    }

    private static void emitTooling(final String wsdlType, Credentials creds) throws Exception {
        final SforceServicePortType port = SalesforceWebServiceUtil.createToolingProxyPort(new SingleSessionMgr(creds), SforceServiceService.class);

        final Map<String, ApexCodeCoverageAggregate> coverageMap = getCodeCoverage(port);

        final Map<String, String> classCoverage = getClassCoverage(port, coverageMap);

        System.out.println("\n\n");
        System.out.println("Class Coverage:");
        System.out.printf("    %40s%13s%13s%15s", " ", "Covered", "Uncovered", "Percent\n");

        for (final String str : classCoverage.values()) {
            System.out.println("    " + str + "%");
        }

        final Map<String, String> triggerCoverage = getTriggerCoverage(port, coverageMap);

        System.out.println("\n\n");
        System.out.println("Trigger Coverage:");
        System.out.printf("    %40s%13s%13s%15s", " ", "Covered", "Uncovered", "Percent\n");

        for (final String str : triggerCoverage.values()) {
            System.out.println("    " + str + "%");
        }
    }

    public static void main(final String[] args) throws Exception {
        LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));

        final String env = "test-dev.properties";
//        final String env = "qa.properties";
//        final String env = "dev.properties";

        long start1 = System.currentTimeMillis();

        emitTooling("Partner WSDL", new PropertiesCredentials(new FilePropertiesMgr(System.getProperty("user.home") + "/.solenopsis/credentials/" + env)));

        long stop1 = System.currentTimeMillis();

        long start2 = System.currentTimeMillis();

        emitTooling("Partner WSDL", new PropertiesCredentials(new FilePropertiesMgr(System.getProperty("user.home") + "/.solenopsis/credentials/" + env)));

        long stop2 = System.currentTimeMillis();

        System.out.println((stop1 - start1) + " -> " + (stop2 - start2));

    }
}
