# Video Window (Fabric, Minecraft 26.1.x)

Taste **U**: Menü öffnen -> Link einfügen -> "Laden".
Orange Leiste ziehen = verschieben, grüne Ecke ziehen = Größe.
Esc: Video läuft als Overlay weiter.

## Jar bekommen (ohne Installation): GitHub Actions
1. Auf github.com ein neues Repository erstellen und ALLE Dateien dieses Ordners hochladen (inkl. .github).
2. Reiter "Actions" -> Workflow "build" abwarten (grüner Haken).
3. Auf den Lauf klicken -> unten "Artifacts" -> herunterladen -> ZIP entpacken.
4. `videomod-1.0.0.jar` (NICHT `-sources`) in den Ordner `.minecraft/mods`.

## Lokal bauen
Java 25 installieren, dann `./gradlew build` -> `build/libs/videomod-1.0.0.jar`.

## Spielen
Fabric Loader für 26.1 + Java 25. Fabric API und MCEF sind in der Jar eingebettet.
Beim 1. Start lädt Chromium einige hundert MB nach.
MCEF Modern steht unter LGPL-2.1 (Lizenzhinweis beim Weitergeben beilegen).
Kein H.264/AAC: YouTube (VP9/AV1) geht, Twitch u.ä. evtl. nicht.
