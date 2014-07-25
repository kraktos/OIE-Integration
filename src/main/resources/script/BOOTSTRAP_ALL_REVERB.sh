#!/bin/bash

#cd /home/arnab/Workspaces/SchemaMapping/linking-IE
cd /home/adutta/git/OIE-Integration/

echo " ========== RUNNING BOOTSTRAP FOR  " $1 " ITERATION " $2 " ============ "

java -jar src/main/resources/executables/BOOTSTRAP_B.jar $1 src/main/resources/input/CONFIG.cfg

cat '/home/adutta/rockit/modelBasic.mln' '/home/adutta/git/OIE-Integration/src/main/resources/output/ds_'$1'/domRanEvidenceBS.db'  > '/home/adutta/rockit/model.mln'

cd /home/adutta/rockit


java -Xmx6G -jar rockit-0.3.228.jar -input model.mln -data '/home/adutta/git/OIE-Integration/src/main/resources/output/ds_'$1'/AllEvidence.db' -output '/home/adutta/git/OIE-Integration/src/main/resources/output/ds_'$1'/outAll.db'

# COPY FILES

cp '/home/adutta/git/OIE-Integration/src/main/resources/output/ds_'$1'/outAll.db' '/home/adutta/git/OIE-Integration/src/main/resources/output/ds_'$1'/out.db'

cp '/home/adutta/git/OIE-Integration/src/main/resources/output/ds_'$1'/domRanEvidenceBS.db' '/home/adutta/git/OIE-Integration/src/main/resources/output/ds_'$1'/domRanEvidence_A'$2'.db'



