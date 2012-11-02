This is an example of compilet project, with tests.

Any new compilet must be an object that extends `scalaxy.Compilet`, and must be listed in the following SPI-style file, one per line:

    src/main/resources/META-INF/services/scalaxy.Compilet
    
To be picked by the Scalaxy compiler plugin, this project must be in the compiler's tool classpath (with `-toolcp`).

See `Usage` for an example of use of this example compilet (not properly tested yet, TODO).
