This is an example of compilet project, with tests.

The compilets defined here perform the following
*   `ConstantReplacements` rewrite any occurrence of the `666` integer into `667`, and any occurrence of `888` into `999`.
*   `JavaDeprecations` throws an error when a call to `Thread.stop` is detected, and warns when a `java.lang.reflect.Field.setAccessible(true)` call is detected.

Any new compilet must be an object that extends `scalaxy.Compilet`, and must be listed in the following SPI-style file, one per line:

    src/main/resources/META-INF/services/scalaxy.Compilet
    
To be picked by the Scalaxy compiler plugin, this project must be in the compiler's tool classpath (with `-toolcp`).

See `Usage` for an example of use of this example compilet (not properly tested yet, TODO).
