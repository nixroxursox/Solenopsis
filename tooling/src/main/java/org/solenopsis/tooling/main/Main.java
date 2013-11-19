package org.solenopsis.tooling.main;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    private static void emitTooling(final String wsdlType, Credentials creds) throws Exception {
        final SforceServicePortType port = SalesforceWebServiceUtil.createToolingPort(creds);

        final Map<String, ApexClass> classMap = getApexClasses(port);
        final Map<String, ApexTrigger> triggerMap = getApexTriggers(port);

        final List<String> classList = new LinkedList<String>();
        final List<String> triggerList = new LinkedList<String>();

        final QueryResult queryResult = port.query(SOQL_COVERAGE);

        for(final SObject sobj : queryResult.getRecords()) {
            ApexCodeCoverage coverage = (ApexCodeCoverage) sobj;

            String name = "";

            List<String> toUse = null;

            if (classMap.containsKey(coverage.getApexClassOrTriggerId())) {
                name = classMap.get(coverage.getApexClassOrTriggerId()).getName();
                toUse = classList;
            } else if (triggerMap.containsKey(coverage.getApexClassOrTriggerId())) {
                name = triggerMap.get(coverage.getApexClassOrTriggerId()).getName();
                toUse = triggerList;
            }

            double cvg = ((double) coverage.getCoverage().getCoveredLines().size() / (coverage.getCoverage().getCoveredLines().size() + coverage.getCoverage().getUncoveredLines().size())) * 10;

            toUse.add(name + " " + cvg);
        }

        System.out.println("Triggers:");

        for (final String str : triggerList) {
            System.out.println("    " + str);
        }

        System.out.println("\nClasses:");

        for (final String str : classList) {
            System.out.println("    " + str);
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
