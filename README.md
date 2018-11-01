# nopmd eclipse plugin
The PMD plugin "ch.acanda.eclipse.pmd.core" contributes and eclipse builder which executes the PMD checks on each build step.
Running PMD on a large workspace can slow down the build process in a significant way.

Maybe, you don't want to execute the PMD checks each time you switch a GIT branch or run the "clean workspace" action.

Then this plugin is for you. 

It contributes two main toolbar buttons "nopmd" and "runpmd". 

The "nopmd" main toolbar button acts as a toggle button which allows to enable or disable the PMD builder globally for all eclipse projects. The enabled state is not persisted, you have to run "nopmd" each time you start eclipse.

The "runpmd" button runs the PMD checks for all the uncommited GIT files in your workspace.

# Installation
- clone this git repository <pre>https://github.com/tobiasmelcher/nopmd.git</pre>
- import the eclipse project "nopmd" into your workspace
- select the "nopmd" project in the project explorer and run context menu action "Export"
- choose then "Plug-in Development" -> "Deployable plug-ins and fragments"
- select combo box "Install into host. Repository:" on the bottom of the upcoming dialog and press "Finish"

# Tested and verified with
eclipse photon (4.8) and plugin version 1.9.0 of "ch.acanda.eclipse.pmd.core"
