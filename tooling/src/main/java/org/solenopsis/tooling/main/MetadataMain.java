/*
 * Copyright (C) 2014 Scot P. Floess
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.solenopsis.tooling.main;

import java.util.logging.LogManager;
import org.flossware.util.properties.impl.FilePropertiesMgr;
import org.solenopsis.lasius.credentials.Credentials;
import org.solenopsis.lasius.credentials.impl.PropertiesCredentials;
import org.solenopsis.lasius.wsimport.session.mgr.impl.SingleSessionMgr;
import org.solenopsis.lasius.wsimport.util.SalesforceWebServiceUtil;
import org.solenopsis.wsdls.metadata.AsyncResult;
import org.solenopsis.wsdls.metadata.DeployOptions;
import org.solenopsis.wsdls.metadata.MetadataPortType;
import org.solenopsis.wsdls.metadata.MetadataService;
import org.solenopsis.wsdls.metadata.RetrieveRequest;

/**
 *
 * @author Scot P. Floess
 */
public class MetadataMain {
    public static void retrieveData(final MetadataPortType port) throws Exception {
        final RetrieveRequest request = new RetrieveRequest();
        
        request.setApiVersion(30.0);
    }
    
    public static void runTests(final MetadataPortType port) throws Exception {
        final DeployOptions options = new DeployOptions();
        
        options.setRunAllTests(true);
        
        final AsyncResult results = port.deploy(new byte[0], options);
        
        System.out.println("ID [" + results.getId() + "] message [" + results.getMessage() + "]  vaule [" + results.getState().value() + "] status code [" + results.getStatusCode().value() + "]");
    }
    
    public static Credentials getCredentials() throws Exception {
        final String env = "sfloess.properties";
        return new PropertiesCredentials(new FilePropertiesMgr(System.getProperty("user.home") + "/.solenopsis/credentials/" + env));
    }

    public static MetadataPortType getPort() throws Exception {
        return SalesforceWebServiceUtil.createMetadataProxyPort(new SingleSessionMgr(getCredentials()), MetadataService.class);
    }    
    
    public static void main(final String[] args) throws Exception {
        LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
        runTests(getPort());
    }
}
