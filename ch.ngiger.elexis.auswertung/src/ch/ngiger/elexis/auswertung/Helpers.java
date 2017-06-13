package ch.ngiger.elexis.auswertung;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import ch.elexis.data.Artikel;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Prescription;
import ch.rgw.tools.JdbcLink.Stm;

public class Helpers {
	
	private static Logger log = LoggerFactory.getLogger(Helpers.class);
	private static ch.rgw.tools.JdbcLink jdbc;
	private static Connection conn = null;
	private static String MediTable = "vem_medi";
	/*
create view vem_info as select vem_kontakt.id, 	 vem_kontakt.Bezeichnung1, vem_kontakt.Bezeichnung2, vem_kontakt.Geburtsdatum,
	vem_medi.info, vem_medi.typ, vem_medi.codename from vem_kontakt, vem_medi
	where vem_medi.id = vem_kontakt.id;
	 */
	/**
	 * @param args
	 */
	public static void addMediAuswertungTable(){
		String query = null;
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			query = "drop table if exists " + MediTable + ";";
			stmt.executeUpdate(query);
			stmt.close();
			stmt = conn.createStatement();
			query =
				"create table if not exists " + MediTable
					+ " ( id Varchar(25),"
					+ " info varchar(255),"
					+ " typ varchar(80),"
					+ " codename varchar(80)); ";
			stmt.executeUpdate(query);
			stmt.close();
			stmt = conn.createStatement();
			query = "drop view if exists vem_info;";
			stmt.executeUpdate(query);
			stmt = conn.createStatement();
			query = "create view vem_info as select"
			+ " vem_kontakt.id, vem_kontakt.Bezeichnung1, vem_kontakt.Bezeichnung2, vem_kontakt.Geburtsdatum,"
			+ " vem_medi.info,   vem_medi.typ,            vem_medi.codename"
			+ " from vem_kontakt, vem_medi"
			+ " where vem_medi.id = vem_kontakt.id;";
			stmt.executeUpdate(query);
			stmt.close();
		} catch (SQLException e1) {
			log.warn("SQLException beim Ausführen von " + query + " " + e1.getLocalizedMessage());
		}
	}

	/*
	 * This is an ugly hack, as we cannot use the UI plugins from ch.elexis.base.article
	 */
	private static String getArtikelName(Prescription item){
		Artikel art = item.getArtikel();
		if (art != null) {
			return item.getArtikel().getLabel();
		} else {
			String name = "Fehler";
			String article_id = item.get(Prescription.FLD_ARTICLE);
			String id = article_id.substring(article_id.indexOf("::") + 2);
			Stm stm = PersistentObject.getConnection().getStatement();
			try {
				String query = "SELECT name FROM artikel WHERE id= '" + id + "'";
				name = stm.queryString(query);
			} finally {
				PersistentObject.getConnection().releaseStatement(stm);
			}
			return name;
		}
	}
	
	/**
	 * @param args
	 */
	@SuppressWarnings("deprecation")
	public static void addFixMediAuswertung(String pat_id){
		jdbc = PersistentObject.getConnection();
		conn = jdbc.getConnection();
		addMediAuswertungTable();
		try {
			Statement sta = conn.createStatement();
			conn.setAutoCommit(false); // To speed up things
			String query = "SELECT ID, Bezeichnung1 FROM kontakt";
			if (pat_id != null) {
				query += " where id = '" + pat_id + "'";
			}
			boolean must_debug = false;
			ResultSet res = sta.executeQuery(query);
			Integer idx = 0;
			while (res.next()) {
				String id = res.getString("ID");
				Patient pat = Patient.load(id);
				if (!pat.istPatient()) {
					log.trace("Kein Patient for id: " + id);
				} else {
					must_debug = id.equals("C385b623f459dadc8032");
					List<Prescription> presc = pat.getMedication(null);
					if (presc.size() > 0) {
						log.debug(pat.getPersonalia() + " fix medit hat " + presc.size() + " Eintraege");
						for (int i = 0; i < presc.size(); i++) {
							Prescription item = presc.get(i);
							if (item.getArtikel() == null) {
								String bem = item.getBemerkung();
								log.trace(id +": getArtikel is null for " +i + " bem: " + bem + " " + item.getBeginDate() + " exists " +item.exists());
								// This does not work + " " + item.exportData());
								// ch.elexis.core.exceptions.PersistenceException: Fehler in der Datenbanksyntax.
							} else {
								String name = item.getArtikel().getName();
								String typ = item.getEntryType().name().toString();
								String codename = item.getArtikel().getCodeSystemName();
								log.debug("info:" + name + " : typ "+ typ + " code " + codename);
								String info =
									getArtikelName(item) + " " + item.getEndDate() + " "
										+ item.getDosis() + " " + item.getBemerkung();
								Statement sta2 = conn.createStatement();
									log.info(id + " info:" + info + " msg " + name);
								String do_insert =
									"insert into " + MediTable + " values ( \"" + id + "\", \""
											+ info +  "\", \"" + typ +  "\", \"" + codename + "\");";
								sta2.executeUpdate(do_insert);
								sta2.close();
							}
						}
						idx += 1;
					}
				}
			}
			log.info("Anzahl Patienten mit Fixmedi: " + idx);
			sta.close();
			conn.setAutoCommit(true);
			conn.close();
		} catch (Exception e) {
			log.warn("Exception: " + e.getMessage());
		}
	}
	
	static void showProgress(String msg){
		System.out.println(msg);
		log.info(msg);
	}
	
	private static void wait_some_time(){
		
		// meaningless eventloop, to avoid exit
		for (int i = 0; i < 1; i++) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
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
			// showProgress("SQLException beim Ausführen von " + query + " " +
			// e1.getLocalizedMessage());
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
			query = "alter table " + table + " add " + fieldname + " text; ";
			stmt.executeUpdate(query);
			stmt.close();
		} catch (SQLException e1) {
			showProgress("SQLException beim Ausführen von " + query + " "
				+ e1.getLocalizedMessage());
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
		String tableCopy = String.format("vem_%1$s", table);
		ArrayList<String> copyStmts = new ArrayList<>();
		copyStmts.add(String.format("DROP TABLE IF EXISTS  %1$s", tableCopy));
		copyStmts.add(String.format("CREATE TABLE %2$s LIKE %1$s", table, tableCopy));
		if (table.toLowerCase().equals("kontakt")) {
			copyStmts.add("alter table " + tableCopy + " add diagnosen_text    text; ");
			copyStmts.add("alter table " + tableCopy + " add sysanamnese_text  text; ");
			copyStmts.add("alter table " + tableCopy + " add famanamnese_text  text; ");
			copyStmts.add("alter table " + tableCopy + " add persanamnese_text text; ");
			copyStmts.add(String.format(
				"INSERT %2$s SELECT  %1$s.*, null, null, null, null FROM %1$s", table, tableCopy));
		} else {
			copyStmts.add(String.format("INSERT %2$s SELECT  %1$s.* FROM %1$s", table, tableCopy));
		}
		// Create statement below works for H2 but not for mysql
		// copyStmts.add(String.format("CREATE TABLE " + tableCopy +
		// " as select all * from %1$s", table));
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
	
	public static void dumpToYaml(String id){
	    //
	    DumperOptions options = new DumperOptions();
	    // options.setWidth(120);
	    // options.setIndent(2);
	    options.setCanonical(true);
		Yaml yaml = new Yaml(options);
		try {
			System.out.println(System.getProperty("user.dir"));
			PrintWriter writer1 = new PrintWriter("demo_db_dump.txt", "UTF-8");
			String query = null;
			Statement stmt = null;
			ResultSet rs = null;
			int j = 0, errors = 0;
			query = "Select * from kontakt";
			if (id != null) {
				query += " where id = '" + id + "'"; // Allow limiting query for debugging
			}
			showProgress("Starting Query " + query);
			String pat_id = null;
			Kontakt patient = null;
			jdbc = PersistentObject.getConnection();
			conn = jdbc.getConnection();
			try {
				stmt =
					conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
						ResultSet.CONCUR_UPDATABLE);
				conn.setAutoCommit(false);
				rs = stmt.executeQuery(query);
				while (rs.next()) {
					j++;
					try {
						pat_id = rs.getString("id");
						patient = Kontakt.load(pat_id);
						yaml.dump(patient, writer1);
					} catch (SQLException e1) {
						errors++;
					}
				}
				conn.setAutoCommit(true);
				stmt.close();
			} catch (SQLException e1) {
				showProgress("addDiagnosesToVemKontakt: vem_kontakt nr "
					+ " id " + pat_id + "\n" + e1.getMessage() + " j: " + j);
			} finally {
				if (stmt != null) {
					try {
						stmt.close();
					} catch (SQLException e) {
						showProgress("Fehler bei stmt.close vem_kontakt " + e.getMessage());
					}
				}
			}
			showProgress(String.format("Table vem_kontakt updated %1$d rows with %2$d errors", j,
				errors));
		} catch (FileNotFoundException | UnsupportedEncodingException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
	}
	
	/**
	 * @param args
	 */
	@SuppressWarnings("deprecation")
	public static String createKontaktCopy(String id){
		jdbc = PersistentObject.getConnection();
		conn = jdbc.getConnection();
		String tableCopy = copy_table("kontakt");
		String[] anArray;
		anArray = new String[1];
		anArray[0] = "SysAnamnese";
		// anArray[1] = "FamAnamnese"; // Does not work as we cannot access ch_elexis_data_tarmedleistung
		// anArray[2] = "risiken"; // Does not work as we cannot access ch_elexis_data_tarmedleistung
		// anArray[3] = "diagnosen"; // Does not work as we cannot access ch_elexis_data_tarmedleistung
		
		for (int i = 0; i < anArray.length; i++) {
			String fieldName = anArray[i];
			System.out.println("Adding field " + fieldName);
			Db_extinfo_updater.jdbcConvertExtInfo("vem_kontakt", fieldName, id);
		}
		wait_some_time();
		return tableCopy;
	}
	
	public static void convertBlobIntoVarchar(String table, String field_name){
		String new_field = field_name + "_text";
		String query = null;
		Statement stmt = null;
		ResultSet rs = null;
		int j = 0;
		String value = "";
		add_field_to_table(table, new_field);
		
		query = "Select * from " + table + " where " + field_name + " is not null ";
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
				// TODO: really convert extinfo
				rs.updateRow();
			}
			stmt.close();
			
		} catch (SQLException e1) {
			showProgress("convertBlobIntoVarchar: SQLException Tabelle " + table + " "
				+ e1.getMessage());
			showProgress(" Query was " + query);
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
		showProgress(String.format("Table %1$s updated %2$d rows with field %3$s", table, j,
			field_name));
		
	}
	
	public static void addDiagnosesToVemKontakt(String table_name, String id){
		String query = null;
		Statement stmt = null;
		ResultSet rs = null;
		int j = 0, errors = 0;
		query = "Select * from " + table_name;
		if (id != null) {
			query += " where id = '" + id + "'"; // Allow limiting query for debugging
		}
		showProgress("Starting Query " + query);
		String pat_id = null;
		Patient patient = null;
		try {
			stmt =
				conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			conn.setAutoCommit(false);
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				j++;
				try {
					pat_id = rs.getString("id");
					patient = Patient.load(pat_id);
					if (j % 500 == 1) {
						log.debug("\naddDiagnosesToVemKontakt: at pat_id  " + pat_id + " j: "
							+ (j - 1));
						conn.commit();
					}
					rs.updateString("diagnosen_text", patient.getDiagnosen());
					rs.updateString("famanamnese_text", patient.getFamilyAnamnese());
					rs.updateString("persanamnese_text", patient.getPersAnamnese());
					/*
					if (!patient.getSystemAnamnese().contains("**ERROR:")) {
						rs.updateString("sysanamnese_text", patient.getSystemAnamnese());
					}
					*/
					rs.updateRow();
				} catch (SQLException e1) {
					showProgress("addDiagnosesToVemKontakt: vem_kontakt nr " + patient.getPatCode()
						+ " id " + pat_id + "\n" + e1.getMessage() + " j: " + j);
					errors++;
				}
			}
			conn.setAutoCommit(true);
			stmt.close();
		} catch (SQLException e1) {
			showProgress("addDiagnosesToVemKontakt: vem_kontakt nr " + patient.getPatCode()
				+ " id " + pat_id + "\n" + e1.getMessage() + " j: " + j);
		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					showProgress("Fehler bei stmt.close vem_kontakt " + e.getMessage());
				}
			}
		}
		showProgress(String.format("Table vem_kontakt updated %1$d rows with %2$d errors", j,
			errors));
	}
}
