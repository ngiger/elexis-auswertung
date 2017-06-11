#!/bin/bash -v
echo "Launching ElexisAuswertung"

# Copy and adapt this batch file for your environment. Hints are
# linux/gtk/x86_64 or   macosx/cocoa/MacOS or win32/win32/x86/ or win32/win32/x86_64/
# dbUser:               username of mysql database
# dbPW:                 password of mysql database
# ch.elexis.dbSpec      replace localhost and elexis by your mysql host and database name
# ch.elexis.username    username when logging in to elexis
# -Dch.elexis.password  password when logging in to elexis

./ch.ngiger.elexis.auswertung.products/target/products/ch.ngiger.elexis.auswertung.EA/macosx/cocoa/x86_64/ElexisAuswertung.app/Contents/MacOS/ElexisAuswertung \
-consoleLog -noExit -debug -vmargs \
-Dch.elexis.dbUser=elexis -Dch.elexis.dbPw=elexisTest \
-Dch.elexis.dbFlavor=mysql  -Dch.elexis.dbSpec=jdbc:mysql://192.168.0.70/elexisdb \
-Dch.elexis.username=007 -Dch.elexis.password=topsecret


