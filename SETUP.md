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
