# Baritone
[![Build Status](https://travis-ci.com/cabaletta/baritone.svg?branch=master)](https://travis-ci.com/cabaletta/baritone)
[![Release](https://img.shields.io/github/release/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/releases)
[![License](https://img.shields.io/badge/license-LGPL--3.0-green.svg)](LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a73d037823b64a5faf597a18d71e3400)](https://www.codacy.com/app/leijurv/baritone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cabaletta/baritone&amp;utm_campaign=Badge_Grade)
[![HitCount](http://hits.dwyl.com/cabaletta/baritone.svg)](http://hits.dwyl.com/cabaletta/baritone)
[![Known Vulnerabilities](https://snyk.io/test/github/cabaletta/baritone/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/cabaletta/baritone?targetFile=build.gradle)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/cabaletta/baritone/issues)
[![Minecraft](https://img.shields.io/badge/MC-1.12.2-green.svg)](https://minecraft.gamepedia.com/1.12.2)
[![Asuna integration](https://img.shields.io/badge/Asuna%20integration-builder%20branch-brightgreen.svg)](https://github.com/EmotionalLove/Asuna/)
[![Impact integration](https://img.shields.io/badge/Impact%20integration-v1.0.0--hotfix--4-green.svg)](https://impactdevelopment.github.io/)
[![KAMI integration](https://img.shields.io/badge/KAMI%20integration-v1.0.0-orange.svg)](https://github.com/zeroeightysix/KAMI/)
[![WWE integration](https://img.shields.io/badge/WWE%20%22integration%22-v1.0.0%3F%3F%20smh%20license%20violations-orange.svg)](https://wweclient.com/)
[![Future integration](https://img.shields.io/badge/Future%20integration-Soon™%3F%3F%3F-red.svg)](https://futureclient.net/)
[![ForgeHax integration](https://img.shields.io/badge/ForgeHax%20integration-Soon™-red.svg)](https://github.com/fr1kin/ForgeHax)

A Minecraft pathfinder bot. 

Baritone is the pathfinding system used in [Impact](https://impactdevelopment.github.io/) since 4.4. There's a [showcase video](https://www.youtube.com/watch?v=yI8hgW_m6dQ) made by @Adovin#3153 on Baritone's integration into Impact. [Here's](https://www.youtube.com/watch?v=StquF69-_wI) a video I made showing off what it can do.

This project is an updated version of [MineBot](https://github.com/leijurv/MineBot/),
the original version of the bot for Minecraft 1.8, rebuilt for 1.12.2. Baritone focuses on reliability and particularly performance (it's over [30x faster](https://github.com/cabaletta/baritone/pull/180#issuecomment-423822928) than MineBot at calculating paths).

Here are some links to help to get started:

- [Features](FEATURES.md)

- [Installation](INSTALL.md)

# Setup

- Clone or download Baritone

  ![Image](https://i.imgur.com/kbqBtoN.png)
  - If you choose to download, make sure you extract the ZIP archive.
- Follow one of the instruction sets below, based on your preference

## Command Line
On Mac OSX and Linux, use `./gradlew` instead of `gradlew`.

Setting up the Environment:

```
$ gradlew setupDecompWorkspace
$ gradlew --refresh-dependencies
```

Running Baritone:

```
$ gradlew runClient
```

For information on how to build baritone, see [Building Baritone](#building-baritone)

## IntelliJ
- Open the project in IntelliJ as a Gradle project
  
  ![Image](https://i.imgur.com/jw7Q6vY.png)

- Run the Gradle tasks `setupDecompWorkspace` then `genIntellijRuns`
  
  ![Image](https://i.imgur.com/QEfVvWP.png)

- Refresh the Gradle project (or, to be safe, just restart IntelliJ)
  
  ![Image](https://i.imgur.com/3V7EdWr.png)

- Select the "Minecraft Client" launch config
  
  ![Image](https://i.imgur.com/1qz2QGV.png)

- Click on ``Edit Configurations...`` from the same dropdown and select the "Minecraft Client" config
  
  ![Image](https://i.imgur.com/s4ly0ZF.png)

- In `Edit Configurations...` you need to select `baritone_launch` for `Use classpath of module:`.
  
  ![Image](https://i.imgur.com/hrLhG9u.png)

# Building

## Command Line

```
$ gradlew build
```

## IntelliJ

- Navigate to the gradle tasks on the right tab as follows

  ![Image](https://i.imgur.com/PE6r9iN.png)

- Right click on **build** and press **Run**

## Artifacts

Building Baritone will result in 3 artifacts created in the ``dist`` directory.

- **API**: Only the non-api packages are obfuscated. This should be used in environments where other mods would like to use Baritone's features.
- **Standalone**: Everything is obfuscated. This should be used in environments where there are no other mods present that would like to use Baritone's features.
- **Unoptimized**: Nothing is obfuscated. This shouldn't be used ever in production.

## More Info
To replace out Impact 4.4's Baritone build with a customized one, switch to the `impact4.4-compat` branch, build Baritone as above then copy `dist/baritone-api-$VERSION$.jar` into `minecraft/libraries/cabaletta/baritone-api/1.0.0/baritone-api-1.0.0.jar`, replacing the jar that was previously there. You also need to edit `minecraft/versions/1.12.2-Impact_4.4/1.12.2-Impact_4.4.json`, find the line `"name": "cabaletta:baritone-api:1.0.0"`, remove the comma from the end, and entirely remove the line that's immediately after (starts with `"url"`). 

# Chat control
[Defined Here](src/main/java/baritone/utils/ExampleBaritoneControl.java)

Quick start example: `thisway 1000` or `goal 70` to set the goal, `path` to actually start pathing. Also try `mine diamond_ore`. `cancel` to cancel.

# API example

```
BaritoneAPI.getSettings().allowSprint.value = true;
BaritoneAPI.getSettings().pathTimeoutMS.value = 2000L;

BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(10000, 20000));
```

# FAQ

## Can I use Baritone as a library in my custom utility client?

Sure! (As long as usage is in compliance with the LGPL 3 License)

## How is it so fast?

Magic. (Hours of [Leijurv](https://github.com/leijurv) enduring excruciating pain)

## Why is it called Baritone?

It's named for FitMC's deep sultry voice. 
