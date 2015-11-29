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

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.admin.AccessControl;
import ch.elexis.core.constants.StringConstants;
import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.constants.ElexisSystemPropertyConstants;
import ch.elexis.core.data.extension.CoreOperationExtensionPoint;
import ch.elexis.core.exceptions.PersistenceException;
import ch.elexis.data.Anwender;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Mandant;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Person;
import ch.elexis.data.Query;

/**
 * The main application class as referenced by the org.eclipse.core.runtime.applications extension
 * point.
 */
public class Application implements IApplication {
	
	private Logger log = LoggerFactory.getLogger(Application.class);
	
	@SuppressWarnings("unused")
	private static void force_password(String username, String password){
		if (username == null || password == null) {
			return;
		}
		if (Anwender.login(username, password)) {
			return;
		}
		Mandant m = new Mandant(username, password);
		String clientEmail = System.getProperty(ElexisSystemPropertyConstants.CLIENT_EMAIL);
		if (clientEmail == null)
			clientEmail = "james@bond.invalid";
		m.set(new String[] {
			Person.NAME, Person.FIRSTNAME, Person.TITLE, Person.SEX, Person.FLD_E_MAIL,
			Person.FLD_PHONE1, Person.FLD_FAX, Kontakt.FLD_STREET, Kontakt.FLD_ZIP,
			Kontakt.FLD_PLACE
		}, "Bond", "James", "Dr. med.", Person.MALE, clientEmail, "0061 555 55 55",
			"0061 555 55 56", "10, Baker Street", "9999", "Elexikon");
		String gprs = m.getInfoString(AccessControl.KEY_GROUPS); //$NON-NLS-1$
		gprs = StringConstants.ROLE_ADMIN + "," + StringConstants.ROLE_USERS;
		m.setInfoElement(AccessControl.KEY_GROUPS, gprs);
	}
	
	@Override
	public Object start(IApplicationContext context) throws Exception{
		// register ElexisEvent and MessageEvent listeners
		log.debug("Registering " + CoreEventListenerRegistrar.class.getName());
		new CoreEventListenerRegistrar();
		
		// Check if we "are complete" - throws Error if not
		CoreOperationExtensionPoint.getCoreOperationAdvisor();
		
		// connect to the database
		try {
			if (PersistentObject.connect(CoreHub.localCfg) == false)
				log.error(PersistentObject.class.getName() + " initialization failed.");
		} catch (PersistenceException pe) {
			log.error("Initialization error", pe);
			pe.printStackTrace();
			System.exit(1);
		}
		// log-in
		String username = System.getProperty(ElexisSystemPropertyConstants.LOGIN_USERNAME);
		String password = System.getProperty(ElexisSystemPropertyConstants.LOGIN_PASSWORD);
		
		// check connection by logging number of contact entries
		Query<Kontakt> qbe = new Query<>(Kontakt.class);
		log.debug("Number of contacts in DB: " + qbe.execute().size());
		
		log.debug("Starting Login as " + username);
		// TODO force_password(username, password); // TODO: This line should always be commented out before a check in!
		if (username != null && password != null) {
			if (!Anwender.login(username, password)) {
				log.error("Authentication failed. Exiting.");
				System.exit(1);
			}
		} else {
			log.error("Does not support interactive log-in, please use system properties");
			System.exit(1);
		}
		
		// check if there is a valid user
		if ((CoreHub.actUser == null) || !CoreHub.actUser.isValid()) {
			// no valid user, exit (don't consider this as an error)
			log.warn("Exit because no valid user logged-in");
			PersistentObject.disconnect();
			System.exit(2);
		}
		
		String id = null; // "C385b623f459dadc8032"; // To debug use eg.   aka  Leimener Julia, aka Testperson ArmesWesen, 1.1.1950
		/* Test with
		 select bezeichnung1, bezeichnung2, Geburtsdatum, risiken, diagnosen_text, sysanamnese_text, famanamnese_text, persanamnese_text 
		 from vem_kontakt where id = 'C385b623f459dadc8032';
		 */
		String tableCopy = Helpers.createKontaktCopy(id);
		// Helpers.removeNonPatiensFromKontaktCopy();
		Helpers.addDiagnosesToVemKontakt(tableCopy, id);
		Helpers.addFixMediAuswertung(id);
		PersistentObject.disconnect();
		System.exit(0);
		return null;
	}
	
	@Override
	public void stop(){}
}
