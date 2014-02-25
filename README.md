mapwriter
=========

MapWriter: A minimap mod for Minecraft


Instructions for development:

1) Set up your Forge/MCP environment.

2) Move or copy the mapwriter folder to forge/mcp/src/minecraft/

3) Copy or move the textures from mapwriter/textures/*.png to
   forge/mcp/bin/minecraft/assets/mapwriter/textures/map/*.png

4) Modify the code, and use recompile.bat and startclient.bat to test.
   Alternatively use Eclipse and recompile and test by pressing the run button.

Reobfuscation and Packaging:

1) Run the recompile.bat script in your mcp directory.

2) Run the reobfuscate.bat script.

3) Create a zip file of the 'forge/mcp/reobf/minecraft/mapwriter' folder.

4) Add the textures to the zip file in the folder
   [MapWriter.zip]/assets/mapwriter/textures/map/*.png
   
   The final structure should look like:
	   MapWriter.zip
	   | assets/mapwriter/textures/map/
	   | | arrow_north.png
	   | | arrow_player.png
	   | | ...
	   |
	   | mapwriter/
	     | api/
	     | forge/
	     | gui/
	     | map/
	     | ...
	     | Mw.class
	     | MwUtil.class
	     | ... 

Acknowledgements:

* Chrixian for the code to get death markers working.
* ProfMobius for the overlay API.
* taelnia for extrautils compatibility patch.
* LoneStar144 for minimap border and arrow textures.
* jk-5 for updating the mod to be compatible with Minecraft 1.7.
