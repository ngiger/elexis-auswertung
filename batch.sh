#!/bin/bash -v
echo "Launching ElexisAuswertung"

# Copy and adapt this batch file for your environment. Hints are
# linux/gtk/x86_64 or   macosx/cocoa/MacOS or win32/win32/x86/ or win32/win32/x86_64/
# dbUser:               username of mysql database
# dbPW:                 password of mysql database
# ch.elexis.dbSpec      replace localhost and elexis by your mysql host and database name
# ch.elexis.username    username when logging in to elexis
# -Dch.elexis.password  password when logging in to elexis

time ./ch.ngiger.elexis.auswertung.products/target/products/ch.ngiger.elexis.auswertung.EA/linux/gtk/x86_64/ElexisAuswertung \
--launcher.suppressErrors \
-vmargs \
-Dch.elexis.dbUser=elexis -Dch.elexis.dbPw=elexisTest \
-Dch.elexis.dbFlavor=mysql  -Dch.elexis.dbSpec=jdbc:mysql://localhost/elexisdb \
-Dch.elexis.username=007 -Dch.elexis.password=topsecret 2>&1 | tee batch.log
