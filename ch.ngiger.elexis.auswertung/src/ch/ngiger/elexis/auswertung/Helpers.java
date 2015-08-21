package ch.ngiger.elexis.auswertung;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.constants.ElexisSystemPropertyConstants;
import ch.elexis.core.exceptions.PersistenceException;
import ch.elexis.data.Anwender;
import ch.elexis.data.Kontakt;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;

public class Helpers {

	private static Logger log = LoggerFactory.getLogger(Helpers.class);
	private static ch.rgw.tools.JdbcLink jdbc;
	private static Connection conn = null;

	/**
	 * @param args
	 */
	@SuppressWarnings("deprecation")
	public static void createKontaktCopyWithDiagnosesText(){
		jdbc = PersistentObject.getConnection();
		conn = jdbc.getConnection();
		String tableCopy = copy_table("kontakt");
		removeNonPatiensFromKontaktCopy();

		String[] anArray;
		anArray = new String[5];
		anArray[0] = "diagnosen";
		anArray[1] = "FamAnamnese";
		anArray[2] = "SysAnamnese";
		anArray[3] = "Risiken";
		anArray[4] = "Allergien";
		// anArray[5] = "extinfo";

		for (int i = 0; i < anArray.length; i++) {
			String fieldName = anArray[i];
			convertBlobIntoVarchar(tableCopy, fieldName);
		}
		wait_some_time();
	}

	static void showProgress(String msg){
		log.info(msg);
	}

	private static void wait_some_time(){

		// meaningless eventloop, to avoid exit
		for (int i = 0; i < 1; i++) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static boolean columnExist(String tableName, String columnName){
		String query = null;
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			query = "SELECT 1, count(" + columnName + ") FROM " + tableName;
			stmt.executeUpdate(query);
			stmt.close();
			return true;
		} catch (SQLException e1) {
			// showProgress("SQLException beim Ausführen von " + query + " " + e1.getLocalizedMessage());
			return false;
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					showProgress("SQLException bei stmt.close" + query);
				}
			}
		}
	}

	private static void add_field_to_table(String table, String fieldname){
		if (columnExist(table, fieldname)) {
			return;
		}
		String query = null;
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			query = "alter table " + table + " add " + fieldname + " varchar (16000); ";
			stmt.executeUpdate(query);
			stmt.close();
		} catch (SQLException e1) {
			showProgress(
				"SQLException beim Ausführen von " + query + " " + e1.getLocalizedMessage());
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					showProgress("SQLException bei stmt.close" + query);
				}
			}
		}
	}

	private static void removeNonPatiensFromKontaktCopy(){
		String query = null;
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate("delete from kontakt_copy where istPatient = false");
			stmt.close();
		} catch (SQLException e1) {
			showProgress("SQLException beim Abrufen von " + query + " " + e1.getLocalizedMessage());
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					showProgress("SQLException bei stmt.close" + query);
				}
			}
		}
	}

	public static String copy_table(String table){
		ArrayList<String> copyStmts = new ArrayList<String>();
		copyStmts.add(String.format("DROP TABLE IF EXISTS %1$s_COPY", table));
		String tableCopy = String.format("%1$s_COPY", table);
		copyStmts.add(String.format("CREATE TABLE " + tableCopy + " LIKE %1$s", table));
		copyStmts.add(String.format("INSERT %1$s_COPY SELECT * FROM %1$s", table));
		String query = null;
		Statement stmt = null;
		try {
			for (int k = 0; k < copyStmts.size(); k++) {
				query = copyStmts.get(k);
				showProgress(query);
				stmt = conn.createStatement();
				stmt.executeUpdate(query);
				stmt.close();
			}
		} catch (SQLException e1) {
			showProgress("SQLException beim Abrufen von " + query + " " + e1.getLocalizedMessage());
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					showProgress("SQLException bei stmt.close" + query);
				}
			}
		}
		return tableCopy;
	}

	private static void loginToDb(){

		// connect to the database
		try {
			if (PersistentObject.connect(CoreHub.localCfg) == false)
				log.error(PersistentObject.class.getName() + " initialization failed.");
		} catch (PersistenceException pe) {
			log.error("Initialization error", pe);
			pe.printStackTrace();
			System.exit(1);
		}
		// check connection by logging number of contact entries
		Query<Kontakt> qbe = new Query<>(Kontakt.class);
		log.debug("Number of contacts in DB: " + qbe.execute().size());
		// log-in
		String username = System.getProperty(ElexisSystemPropertyConstants.LOGIN_USERNAME);
		String password = System.getProperty(ElexisSystemPropertyConstants.LOGIN_PASSWORD);
		log.debug("Starting Login as " + username);
		if (username != null && password != null) {
			if (!Anwender.login(username, password)) {
				log.error("Authentication failed. Exiting.");
				System.exit(1);
			}
		} else {
			log.error("Does not support interactive log-in, please use system properties");
			System.exit(1);
		}
	}

	public static void convertBlobIntoVarchar(String table, String field_name){
		String new_field = field_name + "_text";
		String query = null;
		Statement stmt = null;
		ResultSet rs = null;
		int j = 0;
		String value = "";
		add_field_to_table(table, new_field);

		// query = "Select * from " + table;
		query = "Select * from " + table + " where " + field_name + " is not null";
		showProgress("Starting Query " + query);
		try {
			stmt =
				conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			conn.setAutoCommit(false);

			rs = stmt.executeQuery(query);
			while (rs.next()) {
				j++;
				if (j % 8000 == 1) {
					log.debug(String.format("\n%1$20s: %2$6d ", table, j - 1));
					conn.commit();
				}
				ch.elexis.data.Patient patient = ch.elexis.data.Patient.load(rs.getString("Id"));
				rs.updateString(new_field, patient.getDiagnosen().replaceAll("\r\n", ";"));
				rs.updateRow();
			}
			stmt.close();

		} catch (SQLException e1) {
			showProgress("SQLException beim Updaten der extinfo der Tabelle " + table + " "
				+ e1.getMessage());
			String msg =
				String.format("%1$s %2$d field %3$s value %4$s", table, j, new_field, value);
			showProgress(msg);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					showProgress("Fehler bei stmt.close" + table + " " + e.getMessage());
				}
			}
		}
		showProgress(
			String.format("Table %1$s updated %2$d rows with field %3$s", table, j, field_name));

	}

}
