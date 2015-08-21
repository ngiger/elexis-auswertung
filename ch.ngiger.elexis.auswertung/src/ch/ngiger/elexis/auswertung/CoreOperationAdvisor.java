/*******************************************************************************
 * Copyright (c) 2013 MEDEVIT <office@medevit.at>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     MEDEVIT <office@medevit.at> - initial API and implementation
 ******************************************************************************/
package ch.ngiger.elexis.auswertung;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.data.constants.ElexisSystemPropertyConstants;
import ch.elexis.core.data.extension.AbstractCoreOperationAdvisor;
import ch.elexis.core.data.util.SqlRunner;
import ch.elexis.data.Anwender;

public class CoreOperationAdvisor extends AbstractCoreOperationAdvisor {
	private Logger log = LoggerFactory.getLogger(CoreOperationAdvisor.class);

	@Override
	public void requestDatabaseConnectionConfiguration(){
		log.debug("CoreOperationAdvisor: requestDatabaseConnectionConfiguration()");

	}

	@Override
	public void requestInitialMandatorConfiguration(){
		log.debug("CoreOperationAdvisor: requestInitialMandatorConfiguration()");

	}

	@Override
	public void adaptForUser(){
		log.debug("CoreOperationAdvisor: adaptForUser()");
	}

	@Override
	public boolean openQuestion(String title, String message){
		log.debug("CoreOperationAdvisor: openQuestion() " + title + "\n" + message);
		if (title.contains("Diskrepanz in der Datenbank-Version")) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void performLogin(Object shell){
		String username = System.getProperty(ElexisSystemPropertyConstants.LOGIN_USERNAME);
		String password = System.getProperty(ElexisSystemPropertyConstants.LOGIN_PASSWORD);
		if (username != null && password != null) {
			/* Allow bypassing the login dialog, eg. for automated GUI-tests.
			 * Example: when having a demoDB you may login directly by passing
			 * -vmargs -Dch.elexis.username=test -Dch.elexis.password=test
			 * as command line parameters to elexis.
			 */
			log.error("Bypassing LoginDialog with username " + username);
			if (!Anwender.login(username, password)) {
				log.error("Authentication failed. Exiting");
			}
		}
	}

	@Override
	public String getInitialPerspective(){
		log.debug("CoreOperationAdvisor: getInitialPerspective()");
		return null;
	}

	@Override
	public boolean performDatabaseUpdate(String[] array, String pluginId){
		return new SqlRunner(array, pluginId).runSql();
	}

}
