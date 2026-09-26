# Nodes

Nodes adds breakable resource nodes that respawn on a timer.

## Commands

| Command | Description                                                            |
|---|------------------------------------------------------------------------|
| `/nodes set [name] [seconds]` | Turn the block you're currently looking at into a node                 |
| `/nodes remove [name]` | Delete the node you're currently looking at, or specify by name        |
| `/nodes edit <name> material` | Retype a node to the material of the block you're currently looking at |
| `/nodes edit <name> respawn <seconds>` | Change a specific node's respawn time                                  |
| `/nodes list` | List every node (click to teleport)                                    |
| `/nodes tp <name>` | Teleport to a node                                                     |
| `/nodes reset <name\|all>` | End the respawn timer of a node or all nodes early                     |
| `/nodes reload` | Reload config and nodes                                                |

## Config

```yaml
nodes:
  default-respawn-seconds: 60
  broken-material: BEDROCK
  set-range: 5
  protect: true
  drop-items: true
  restore-on-disable: true
  autosave-seconds: 60
  hide-visuals-below-seconds: 0.5

display:
  enabled: true
  title: "&e&lRESPAWNING"
  update-ticks: 20
  y-offset: 1.5
  bar-length: 20
  view-range: 32

messages:
  prefix: "&7[&bNodes&7] &r"
```
