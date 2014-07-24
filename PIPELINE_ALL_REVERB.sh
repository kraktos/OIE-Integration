#!/bin/bash

# CHANGE TO THE RELEVAT DIRECTORY
cd /home/adutta/git/OIE-Integration/

if [ ! -d 'src/main/resources/output/ds_'$1'' ]; then
	mkdir 'src/main/resources/output/ds_'$1''
fi

echo " --------- RUNNING FULL REASONING FOR " $1


# trying out only the Alpha tree
java -jar /home/adutta/git/OIE-Integration/PIPE_ALLREVERB.jar $1 /home/adutta/git/OIE-Integration/CONFIG.cfg


#DYNAMICALLY CREATE THE MODEL FILE

###cat '/home/arnab/Work/rockit/modelBasic.mln' '/home/arnab/Workspaces/SchemaMapping/linking-IE/resource/output/ds_'$1'/domRanEvidence.db'  > '/home/arnab/Work/rockit/model.mln'

#CHANGE TO ROCKIT DIRECTORY
###cd /home/arnab/Work/rockit

# RUN INFERENCE ENGINE

###java -Xmx4G -jar rockit-0.3.228.jar -input model.mln -data '/home/arnab/Workspaces/SchemaMapping/linking-IE/resource/output/ds_'$1'/AllEvidence.db' -output '/home/arnab/Workspaces/SchemaMapping/linking-IE/resource/output/ds_'$1'/outAll.db'

# COPY FILES


###cp '/home/arnab/Workspaces/SchemaMapping/linking-IE/resource/output/ds_'$1'/outAll.db' '/home/arnab/Workspaces/SchemaMapping/linking-IE/resource/output/ds_'$1'/out.db'

###cp '/home/arnab/Workspaces/SchemaMapping/linking-IE/resource/output/ds_'$1'/domRanEvidence.db' '/home/arnab/Workspaces/SchemaMapping/linking-IE/resource/output/ds_'$1'/domRanEvidence_A1.db'

