mapwriter
=========

MapWriter: A minimap mod for Minecraft


Instructions for development:

1) Run "gradlew setupDecompWorkspace" in the mapwriter folder.

2) Run "gradlew eclipse" if using the eclipse IDE. If it does not work you may
   need to copy the eclipse folder from a Forge src release into the mapwriter
   folder and retry the command.

3) Open eclipse and set the workspace directory to mapwriter/eclipse.

4) You should now be able to modify the code and test by using the "Client" Run
   Configuration.

Reobfuscation and Packaging:

1) Edit the version numbers in mapwriter.forge.MwForge and build.gradle.
   The version numbers in mcmod.info should automatically be set to the same
   versions as set in the build.gradle file.

2) Run "gradlew reobf".

3) The reobfuscated jar should be output to the mapwriter/build/libs folder.

Acknowledgements:

* Chrixian for the code to get death markers working.
* ProfMobius for the overlay API.
* taelnia for extrautils compatibility patch.
* LoneStar144 for minimap border and arrow textures.
* jk-5 for updating the mod to be compatible with Minecraft 1.7.
