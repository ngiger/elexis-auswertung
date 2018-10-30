#!/bin/bash -v
#   Run the batch inside a docker and generate a timestamp log file
# Copy and adapt this batch file for your environment. Hints are
# dbUser:               username of mysql database
# dbPW:                 password of mysql database
# ch.elexis.dbSpec      replace localhost and elexis by your mysql host and database name
# ch.elexis.username    username when logging in to elexis
# -Dch.elexis.password  password when logging in to elexis
LOGFILE=/var/log/auswertung/docker-`date +%Y.%m.%d-%H.%M.%S`.log
echo "Launching ElexisAuswertung using xvfb-run" > $LOGFILE 
echo "Starting at `date`" >> $LOGFILE 
xvfb-run /usr/local/auswertung/ElexisAuswertung \
--launcher.suppressErrors \
-vmargs \
-Dch.elexis.dbUser=elexis -Dch.elexis.dbPw=elexisTest \
-Dch.elexis.dbFlavor=mysql  -Dch.elexis.dbSpec=jdbc:mysql://192.168.0.70:1400/bruno \
-Dch.elexis.username=Administrator -Dch.elexis.password=admin 2>&1 | tee --append $LOGFILE
echo "Finished at `date`" >> $LOGFILE 

# mysql --host=192.168.0.70 --port=1400 --user=elexis --password=elexisTest elexisdb
