OIE-Integration
===============

Installation
===================

Linux OS and Eclipse IDE

Requirements
1. Have the  jdk higher than the version 1.7 installed. Not jre but jdk !!
2. Have maven installed, atleast maven2 
3. have a github account


Project Setup

1. Clone the repository https://github.com/kraktos/OIE-Integration.git from the eclipse "Git Repository Exploring" view. 
Say for example, ur local repository is /home/user/Workspaces/Projects/OIE-Integration
a .git folder should be created there after cloning

2. Change perspective to java and import project

3. Must select an existing Maven Project from the options. 

4. Browse to the folder in your machine where the project is freshly cloned in Step 1. 
/home/user/Workspaces/Projects/OIE-Integration in this case

5. open command prompt and cd to /home/user/Workspaces/Projects/OIE-Integration and run the command
  mvn clean compile assembly:single install

