package ch.ngiger.elexis.auswertung;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.data.PersistentObject;

public class Activator extends AbstractUIPlugin {
	private final Converter dataLoader;
	private static Logger log = LoggerFactory.getLogger(Activator.class);
	
	public Activator(){
		// TODO Auto-generated constructor stub
		log.debug("ch.elexis.auswertung.Activator Starting");
		dataLoader = new Converter();
		dataLoader.schedule();
	}
	class Converter extends Job {

		public Converter(){
			super("Converter");
		}

		@Override
		protected IStatus run(IProgressMonitor monitor){
			// TODO Auto-generated method stub
			try {
				Thread.sleep(10*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			runConversion();
			finish();
			return null;
		}
	}

	private void runConversion() {
		String id = null; // "C385b623f459dadc8032"; // To debug use eg.   aka  Leimener Julia, aka Testperson ArmesWesen, 1.1.1950
		/* Test with
		 select bezeichnung1, bezeichnung2, Geburtsdatum, risiken, diagnosen_text, sysanamnese_text, famanamnese_text, persanamnese_text
		 from vem_kontakt where id = 'C385b623f459dadc8032';
		 */
		String tableCopy = Helpers.createKontaktCopy(id);
		Helpers.addDiagnosesToVemKontakt(tableCopy, id);
		Helpers.addFixMediAuswertung(id);
	}
	private void finish() {
		PersistentObject.disconnect();
		System.exit(1);
	}
	
}
