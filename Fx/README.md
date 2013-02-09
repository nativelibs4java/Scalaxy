Minimal set of macros and implicits for maximal JavaFX eye-candy!

    import scalaxy.fx._
    
    import javafx._
    import javafx.event._
    
    object HelloWorld extends App {
      Application.launch(classOf[HelloWorld])
    }
    
    class HelloWorld extends Application {
      override def start(primaryStage: Stage) {
        primaryStage.set(
          title = "Hello World!",
          scene = new Scene(
            new StackPane() {
              getChildren.add(
                new Button().set(
                  text = "Say 'Hello World'",
                  onAction = {
                    println("Hello World!")
                  }
                )
              )
            }, 
            300, 
            250
          )
        )
        primaryStage.show()
      }
    }
