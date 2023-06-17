@echo off
mvn -q compile && ^
mvn -q exec:java -Dexec.args="%1 %2"