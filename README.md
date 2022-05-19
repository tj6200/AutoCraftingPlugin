# AutoCraftingPlugin

**Made with the help of the following projects.**
1) https://github.com/Aeltumn/automated_crafting
2) https://github.com/Fliens/AutoCraft

An open-source plugin for Bukkit which adds autocrafters capable of automatically crafting anything.

<br/>

**How to use (when using default configuration settings)**

1) Place a dispenser in the world.

![Step 1](https://i.ibb.co/1dC2qrD/2022-01-13-16-40-56.png)


2) Put an item frame on any side of the dispenser.

![Step 2](https://i.ibb.co/5xr0yz4/2022-01-13-16-41-21.png)


3) Put the item you want to craft in the item frame.

![Step 3](https://i.ibb.co/rxPHJjR/2022-01-13-16-42-34.png)

4) Fill up the dispenser with the crafting ingredients.

![Step 4](https://i.ibb.co/0mxSTST/2022-01-13-16-44-57.png)

5) Place a chest in front of the dispenser.

![Step 5](https://i.ibb.co/R62fPGQ/2022-01-13-16-42-56.png)

6) Enjoy your new autocrafter!

<br/>

**Features**
- Autocrafters will put the items inside of containers adjacent to the face of the dropper. This saves performance for servers so players don't need to have hoppers to pick up the items. 
- Powering the dropper will stop the autocrafting process even if there are still items inside. 
- Optional configuration to show green particles when an autocrafter is crafting an item. 
- Ability to set how fast autocrafters will craft items. 
- Ability to set how long (in minutes) between saves of data. 
- Various redstone modes 
  * Disabled - redstone will not affect the autocrafters. 
  * Direct - autocrafters are disabled with direct power. 
  * Indirect - autocrafters can be disabled with indirect redstone power.
- Commands
  * /reloadrecipes - reloads the recipes saved for the AutoCrafters. Needed if you use a plugin that changes recipes during gameplay.
  * /listcrafters [inChunk] - lists all AutoCrafters created or in the chunk that the player who calls the command is in.
  * /restartcrafters [inChunk] - attempts to restart all AutoCrafters created or in the chunk that the player who calls the command is in.
  * /debugcrafters [true/false] - toggles AutoCrafter debug output for the player that calls the command.



[//]: # (**Adding this plugin as a dependency**)

[//]: # ()
[//]: # (If you want to use this plugin as a dependency. You can use a very handy service called [**jitpack.io**]&#40;https://jitpack.io/&#41;. <br/>)

[//]: # (This services makes it easy to add any git repository as a dependency.)

[//]: # ()
[//]: # (_Gradle_<br/>)

[//]: # (For Gradle you'll need to add the following six lines to your _build.gradle_ file:)

[//]: # (```gradle)

[//]: # (repositories {)

[//]: # (     maven { url 'https://jitpack.io' })

[//]: # (})

[//]: # (dependencies {)

[//]: # (    implementation 'com.github.Aeltumn:automated_crafting:main-SNAPSHOT')

[//]: # (})

[//]: # (```)

[//]: # ()
[//]: # (_Maven_<br/>)

[//]: # (For Maven you can add the following lines to your _pom.xml_ file:)

[//]: # (```xml)

[//]: # (<repositories>)

[//]: # (    <repository>)

[//]: # (        <id>jitpack.io</id>)

[//]: # (        <url>https://jitpack.io</url>)

[//]: # (    </repository>)

[//]: # (</repositories>)

[//]: # ()
[//]: # (<dependency>)

[//]: # (    <groupId>com.github.Aeltumn</groupId>)

[//]: # (    <artifactId>automated_crafting</artifactId>)

[//]: # (    <version>main-SNAPSHOT</version>)

[//]: # (</dependency>)
[//]: # (```)
