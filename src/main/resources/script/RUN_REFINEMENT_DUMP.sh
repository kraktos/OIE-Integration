#!/bin/bash

# CHANGE TO THE RELEVAT DIRECTORY
cd /home/adutta/git/OIE-Integration/


#echo "\n\n ======= RUNNING FULL REASONING FOR " $1 " ========"


# running full pipeline
java -jar src/main/resources/executables/REFINEMENT_LOAD.jar src/main/resources/input/CONFIG.cfg


