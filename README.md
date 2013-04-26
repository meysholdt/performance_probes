performance_probes
==================

Yourkit probes to measure performance of EMF/Xtext/Xtend


How to Start Using the Probes
=============================

1. Install Yourkit.
2. Make sure you have the Xtext sources in your workspace or the the Xtext plugins in your target platform.
3. Clone this repository and import all eclipse project you find in there.
4. Open the project com.yourkit.lib and put the yjp.jar from your Yourkit installation into the lib/ folder.
5. Launch a runtime Workbench via "Profile" (instead of "Run" or "Debug").
6. Do Something in your runtime Workbench, e.g. open an Xtend file. Probes may not be dispayed untill they collected some data.
7. In the Yourkit window, go to the "Probes" tab and inspect your data.
