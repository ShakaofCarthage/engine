# engine

Engine module includes all the source for executing player orders, conducting battles (tactical, strategic, naval), perform the book keeping of the game entities, play the NPCs and in general all the necessary functions required for a for turn-based strategy game engine and client.

## Requirements

1. Oracle Java 1.7 or newer
2. Maven 3
3. A working internet connection
4. The latest stable release of EaW1805/data artifact
5. The latest stable release of EaW1805/algorithms artifact

## Configuration

No particular configuration is necessary apart from the username/password for accessing the mysql database.
These are provided as environmental properties (dbusername and dbpassword), that are passed using the -D<property>=<value>.

## Execution

The engine supports two modes of execution:
1. Manual - we state the scenario and the game that we wish to process and the engine starts the processing of the game.
If we wish to create a new game (for a given scenario) we pass 0 for the game ID.
2. Automatic - we state the scenario and we pass -1 for game ID. The engine will check the deadlines of the games and
will process all games whose deadline is in the past. If we pass -1000, the engine will check for any custom game that
is queued for creation.

The parameters are provided as environmental properties (scenarioId and gameId). In addition to the above parameters,
 two additional parameters are required:
- basePath - the path in the file system where the EaW1805 resources are kept (e.g., game images, log files, etc.)
- buildNumber - usually a unique identifier for the particular invocation of the engine. This is used for logging
purposes to distinguish each engine invocation. If 0 is passed, the engine is executed in silent mode - no emails will
be sent, no tweets will be made, no achievements/hall-of-fame entries will be made, no VPs will be awarded and no
credits will be deducted from user accounts.

For example:

```
mvn3 -Ddbusername=example -Ddbpassword=mypassword -DscenarioId=1 -DgameId=5 -DbasePath=/srv/eaw1805 -DbuildNumber=1833 engine-run
```

If you are unsure about the settings please contact ichatz@gmail.com


## Maven Repository

The artifacts of the project are publicly available by the maven repository hosted on github.

Configure any poms that depend on the engine artifact by adding the following snippet to your pom file:

```
<repositories>
    <repository>
        <id>EaW1805-engine-mvn-repo</id>
        <url>https://raw.github.com/EaW1805/engine/mvn-repo/</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>
```

## Structure

The module comprises of the following main components:

1. core - contains the entry points for the processing engine and tools
2. battles - includes three sub-engines: (a) tactical battles, (b) field battles, (c) naval battles.
3. economy - all the game logic for advancing the game: production, unit maintenance, trade.
4. events - a generic event execution engine along with a large collection of game-related events (army related,
 espionage, fleet related, random events, rumours etc.)
5. map and fieldmap - map drawing components and entry-point classes for stand-alone generation of maps.
6. orders - all the game logic for processing player orders. includes a generic order execution engine along with
a large collection of already-implemented game orders.

The resources include the hibernate configuration files (xml) for the ORM.
