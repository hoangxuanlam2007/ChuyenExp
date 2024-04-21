# ChuyenExp - Minecraft Plugin for in-game Experience Transfer

ChuyenExp is a Paper plugin for Minecraft servers that allows players to transfer experience points (exp) to other players.

## Features

- Transfer experience points (exp) from one player to another.
- Cooldown system to prevent spamming of the command.
- Maximum transfer amount setting to limit the amount of exp that can be transferred at once.
- Confirmation system to prevent accidental transfers.

## Installation

1. Download the latest release from the [Releases](https://github.com/hoangxuanlam2007/ChuyenExp/releases) page.
2. Place the downloaded `ChuyenExp.jar` file into the `plugins` folder of your server.
3. Restart or reload your server.

## Commands

- `/cexp <player> <amount>` - Transfer experience to another player.
- `/cexp help` - Show command help.
- `/cexp reload` - Reload the plugin configuration.

## Permissions

- `chuyenexp` - Base permission.
- `chuyenexp.use` - Allows the usage of `/cexp` command.
- `chuyenexp.bypass.cooldown` - Bypasses the cooldown restriction.
- `chuyenexp.bypass.maxtransfer` - Bypasses the maximum transfer amount restriction.
- `chuyenexp.reload` - Allows reloading the plugin configuration.

## Configuration

The plugin comes with a `config.yml` and a `custom_messages.yml` file which you can find in the plugin's data folder.

## Default Messages

You can customize the plugin's messages by editing the `custom_messages.yml` file.

## Contact

For further information, to report bugs, or to contact the author, please email [chim31102007@gmail.com](mailto:chim31102007@gmail.com).

Author's GitHub: [hoangxuanlam2007](https://github.com/hoangxuanlam2007)
