# Baritone
[![Build Status](https://travis-ci.com/cabaletta/baritone.svg?branch=master)](https://travis-ci.com/cabaletta/baritone)
[![Release](https://img.shields.io/github/release/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/releases)
[![License](https://img.shields.io/badge/license-LGPL--3.0-green.svg)](LICENSE)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a73d037823b64a5faf597a18d71e3400)](https://www.codacy.com/app/leijurv/baritone?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cabaletta/baritone&amp;utm_campaign=Badge_Grade)
[![HitCount](http://hits.dwyl.com/cabaletta/baritone.svg)](http://hits.dwyl.com/cabaletta/baritone)
[![Known Vulnerabilities](https://snyk.io/test/github/cabaletta/baritone/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/cabaletta/baritone?targetFile=build.gradle)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/cabaletta/baritone/issues)
[![Issues](https://img.shields.io/github/issues/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/issues/)
[![GitHub issues-closed](https://img.shields.io/github/issues-closed/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/issues?q=is%3Aissue+is%3Aclosed)
[![Pull Requests](https://img.shields.io/github/issues-pr/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/pulls/)
![GitHub repo size](https://img.shields.io/github/repo-size/cabaletta/baritone.svg)
[![Minecraft](https://img.shields.io/badge/MC-1.12.2-green.svg)](https://minecraft.gamepedia.com/1.12.2)
[![GitHub contributors](https://img.shields.io/github/contributors/cabaletta/baritone.svg)](https://github.com/cabaletta/baritone/graphs/contributors/)
[![GitHub commits](https://img.shields.io/github/commits-since/cabaletta/baritone/v1.0.0.svg)](https://github.com/cabaletta/baritone/commit/)
[![Asuna integration](https://img.shields.io/badge/Asuna%20integration-builder%20branch-brightgreen.svg)](https://github.com/EmotionalLove/Asuna/)
[![Impact integration](https://img.shields.io/badge/Impact%20integration-v1.0.0--hotfix--4-green.svg)](https://impactdevelopment.github.io/)
[![KAMI integration](https://img.shields.io/badge/KAMI%20integration-v1.0.0-orange.svg)](https://github.com/zeroeightysix/KAMI/)
[![WWE integration](https://img.shields.io/badge/WWE%20%22integration%22-v1.0.0%3F%3F%20smh%20license%20violations-orange.svg)](https://wweclient.com/)
[![Future integration](https://img.shields.io/badge/Future%20integration-Soon™%3F%3F%3F-red.svg)](https://futureclient.net/)
[![ForgeHax integration](https://img.shields.io/badge/ForgeHax%20integration-Soon™-red.svg)](https://github.com/fr1kin/ForgeHax)
[![forthebadge](https://forthebadge.com/images/badges/built-with-swag.svg)](http://forthebadge.com)
[![forthebadge](https://forthebadge.com/images/badges/mom-made-pizza-rolls.svg)](http://forthebadge.com)


A Minecraft pathfinder bot. 

Baritone is the pathfinding system used in [Impact](https://impactdevelopment.github.io/) since 4.4. There's a [showcase video](https://www.youtube.com/watch?v=yI8hgW_m6dQ) made by @Adovin#3153 on Baritone's integration into Impact. [Here's](https://www.youtube.com/watch?v=StquF69-_wI) a video I made showing off what it can do.

This project is an updated version of [MineBot](https://github.com/leijurv/MineBot/),
the original version of the bot for Minecraft 1.8, rebuilt for 1.12.2. Baritone focuses on reliability and particularly performance (it's over [30x faster](https://github.com/cabaletta/baritone/pull/180#issuecomment-423822928) than MineBot at calculating paths).

Here are some links to help to get started:

- [Features](FEATURES.md)

- [Setup](SETUP.md)

- [Installation](INSTALL.md)

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
