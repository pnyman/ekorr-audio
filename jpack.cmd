@echo off

call mvn -e clean package -DskipTests
if errorlevel 1 exit /b 1

rmdir /s /q .\target\jpackage-input 2>nul
mkdir .\target\jpackage-input
copy .\target\ekorraudio-1.0-SNAPSHOT-shaded.jar .\target\jpackage-input\
xcopy /e /i /y .\packaging\mpv .\target\jpackage-input\mpv

jpackage --type msi ^
         --input .\target\jpackage-input ^
         --dest . ^
         --main-jar ekorraudio-1.0-SNAPSHOT-shaded.jar ^
         --main-class se.camabomedia.ekorraudio.Main ^
         --module-path "C:\Program Files (x86)\Java\javafx-jmods-17.0.7" ^
         --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.media,java.base,java.desktop,java.logging,java.net.http,java.scripting,jdk.jfr,jdk.jsobject,jdk.unsupported,jdk.xml.dom ^
         --win-shortcut ^
         --win-menu ^
         --win-dir-chooser ^
         --name Ekorr+ ^
         --app-version 0.001 ^
         --icon src\main\resources\se\camabomedia\ekorraudio\icon.ico ^
         --description "Skapa korrektur-rapporter i talboksproduktion" ^
         --copyright "Copyright 2026, All rights reserved"
