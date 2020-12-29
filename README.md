## IMPORTANT

This is the MapWriter mod version 2.1.1 (master branch) for Minecraft 1.7.10,
backported to Minecraft 1.4.7 used in Tekkit Lite.

Because the tools for making 1.4.7 mods are \~7 years old, they may break. These
are the issues I encountered when setting up MCP and how to fix them:

- All scripts fail to run because of syntax errors:
  - All scripts use Python 2, but most systems now use Python 3 by default.
    Switch the system default to use Python 2 instead of Python 3, or manually
    change all scripts to explicitly use `python2` instead of `python` as the
    executable.

- Forge install script fails to download mcp726a.zip:
  - The MCP download links are dead. Use a newer one. The one provided in the
    Minecraft Wiki is
    `https://download1335.mediafire.com/0cwqjrbuz9lg/07d59w314ewjfth/mcp726a.zip`
    and it worked fine for me as of 2020. The link can be changed in
    `forge/fml/mc_versions.cfg`, scroll down to the `[1.4.7]` section and change
    the field `mcp_url` to the new URL.

### Original README with instructions for building with MCP, slightly updated:

mapwriter
=========

MapWriter: A minimap mod for Minecraft


Instructions for development:

1) Set up your Forge/MCP environment.

2) Move or copy the mapwriter folder found in `src/main/java` to
   `forge/mcp/src/minecraft/`

3) Copy or move the textures from
   `src/main/resources/assets/mapwriter/textures/map/*.png` to
   `forge/mcp/bin/minecraft/assets/mapwriter/textures/map/*.png`

4) Modify the code, and use recompile.bat and startclient.bat to test.
   Alternatively use Eclipse and recompile and test by pressing the run button.

Reobfuscation and Packaging:

1) Run the recompile.bat script in your mcp directory.

2) Run the reobfuscate.bat script.

3) Create a zip file of the `forge/mcp/reobf/minecraft/mapwriter` folder.

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

5) Optionally, add `mcmod.info` to the archive so that there is Forge mod info.
   This file is in `resources/mcmod.info`

Acknowledgements:

* Chrixian for the code to get death markers working.
* ProfMobius for the overlay API.
* taelnia for extrautils compatibility patch.
* LoneStar144 for minimap border and arrow textures.
