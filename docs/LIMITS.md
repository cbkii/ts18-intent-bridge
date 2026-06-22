# Limits and safety boundary

## Possible

- Rewrite `Intent` objects before the caller sends them.
- Make `PackageManager` lookups in scoped caller processes delegate from stock package names to replacement package names.
- Preserve original intent extras, type, data, categories and flags where possible.

## Not actually possible with this module alone

- A normal APK cannot acquire another package's real Linux UID.
- A normal APK cannot become platform-signed.
- Root/Magisk/LSPosed do not grant the vendor signing key.
- Provider authorities must be unique in the real package database.
- Binder services and private Topway services cannot be impersonated safely without exact protocol evidence.
- SAF URI grants cannot be faked generically; the destination picker must actually return a grantable URI.

## STOP conditions

Stop testing and disable the module if any of these occur:

- Boot loops, black screen, SystemUI crash, or launcher crash after enabling `android` scope.
- Radio audio path, reverse camera, steering keys, or ACC wake behaviour regresses.
- DoFun launcher repeatedly restarts.
- PackageManager throws signature or UID errors in loops.
- MiXplorer opens but caller receives no result URI.
