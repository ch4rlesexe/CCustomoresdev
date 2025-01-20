# HyTeleport

HyTeleport is a Minecraft plugin that provides enhanced teleportation functionality for Spigot/Paper servers.

## Features
- Custom teleport commands
- Configurable teleportation settings
- Smooth teleport transitions
- Supports permissions for teleportation access

## Installation
1. Download the latest release of HyTeleport.
2. Place the `HyTeleport.jar` file in your server's `plugins` folder.
3. Restart or reload your server.

## Commands
| Command | Description |
|---------|-------------|
| `/htp <player>` | Teleports to a player. |
| `/htphere <player>` | Teleports a player to you. |
| `/htpspawn` | Teleports to spawn. |
| `/htpworld <world>` | Teleports to another world. |

## Permissions
| Permission | Description |
|------------|-------------|
| `hyteleport.tp` | Allows using `/htp`. |
| `hyteleport.tphere` | Allows using `/htphere`. |
| `hyteleport.spawn` | Allows using `/htpspawn`. |
| `hyteleport.world` | Allows using `/htpworld`. |

## Configuration
The plugin provides a `config.yml` file where you can customize teleport settings such as delays, messages, and restrictions.

## Development
### Building from Source
1. Clone the repository:
   ```sh
   git clone https://github.com/your-repo/HyTeleport.git
Navigate to the project directory:
sh
Copy
Edit
cd HyTeleport
Build the plugin using Maven:
sh
Copy
Edit
mvn clean package
The compiled .jar file will be located in target/.
