#!/bin/bash

cd /home/arnab/Workspaces/SchemaMapping/OIE-Integration/target

echo "lakeinstate"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar lakeinstate 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar lakeinstate 2 F

echo "actorstarredinmovie"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar actorstarredinmovie 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar actorstarredinmovie 2 F

echo "agentcollaborateswithagent"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar agentcollaborateswithagent 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar agentcollaborateswithagent 2 F

echo "animalistypeofanimal"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar animalistypeofanimal 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar animalistypeofanimal 2 F

echo "athleteledsportsteam"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar athleteledsportsteam 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar athleteledsportsteam 2 F

echo "bankbankincountry"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar bankbankincountry 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar bankbankincountry 2 F

echo "citylocatedinstate"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar citylocatedinstate 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar citylocatedinstate 2 F


echo "bookwriter"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar bookwriter 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar bookwriter 2 F

echo "companyalsoknownas"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar companyalsoknownas 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar companyalsoknownas 2 F

echo "personleadsorganization"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar personleadsorganization 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar personleadsorganization 2 F

echo "teamplaysagainstteam"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar teamplaysagainstteam 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar teamplaysagainstteam 2 F

echo "weaponmadeincountry"
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar weaponmadeincountry 2 T
java -jar OIE-Integration-0.0.1-SNAPSHOT-jar-with-dependencies.jar weaponmadeincountry 2 F


mv /home/arnab/Workspaces/SchemaMapping/OIE-Integration/target/*.log /home/arnab/Workspaces/SchemaMapping/OIE-Integration/src/main/resources/output/
