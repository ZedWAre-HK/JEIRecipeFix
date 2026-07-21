# Fabric Configuration Sync

## Purpose

Fabric JEI distinguishes recipes synchronized by a server from recipes it loaded from the local client resources. A Paper or Purpur server normally finishes its recipe update before JEIRecipeFix can send a Fabric recipe-sync payload during `PlayerJoinEvent`. JEI has already selected its local fallback recipe set at that point and displays this warning:

```text
This Purpur server does not provide recipes to JEI.
```

JEIRecipeFix now performs Fabric's configuration-stage handshake and injects the recipe-sync payload immediately before the vanilla `ClientboundUpdateRecipesPacket`. Fabric API sets its synchronized client recipe state before JEI starts, so the warning is not shown.

## Scope

This is a protocol-timing fix. It does not suppress JEI chat messages, pretend that Purpur is a Fabric server, install JEI on the server, or alter crafting behavior. The existing join-time synchronization remains as the fallback for cores without the required configuration-stage API.

The handshake is enabled only after the client sends Fabric's `fabric:recipe_sync/supported_serializers` confirmation. Vanilla and unsupported clients do not receive the early Fabric recipe payload.

## Flow

1. During the configuration phase, the plugin advertises the Fabric recipe-sync serializer channel.
2. A Fabric API client confirms its supported recipe serializers after `SelectKnownPacks`.
3. The plugin waits for the server's vanilla recipe update packet.
4. Immediately before that packet, it sends `fabric:recipe_sync` using the same recipe payload used by the normal Fabric sync path.
5. Fabric API applies the synchronized recipes before JEI verifies the client recipe source.

## Confirmed Compatibility

| Server core | Minecraft | Client stack | Result |
| --- | --- | --- | --- |
| Purpur | 1.21.11 | Fabric API 0.141.5+1.21.11, JEI 27.17.0.50 | JEI recipe sync, recipe transfer, and the removal of the Purpur recipe-availability warning verified |

No other core, Minecraft, Fabric API, or JEI combination is claimed as supported for this path yet.

## Verification

1. Install only one copy of the JEIRecipeFix JAR and restart the server.
2. Confirm the startup log includes `Fabric configuration-phase recipe sync is active.`
3. Connect with the tested Fabric client through a fresh server connection.
4. Confirm JEI does not show the Purpur recipe-availability warning.
5. Confirm a JEI recipe transfer still fills the player 2x2 grid or a vanilla crafting table.

If the startup log says the core has no configuration-phase plugin messaging API, normal join-time recipe synchronization still runs, but the early-sync warning fix is unavailable.

## Reporting Another Working Combination

Please use the `Fabric configuration sync compatibility report` issue template. Include the server core and build, Minecraft version, Fabric Loader and Fabric API versions, JEI version, and whether the warning remained absent after reconnecting.
