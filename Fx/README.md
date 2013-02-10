Minimal set of Scala 2.10 macros, dynamics and implicits for maximal JavaFX eye-candy!

    import scalaxy.fx._
    
    import javafx._
    import javafx.event._
    
    object HelloWorld extends App {
      Application.launch(classOf[HelloWorld], args: _*)
    }
    
    class HelloWorld extends Application {
      override def start(primaryStage: Stage) {
        val slider = new Slider().set(
          min = 0,
          max = 100,
          blockIncrement = 1,
          value = 50
        )
        slider.valueProperty onChange {
          println("Slider changed")
        }
        primaryStage.set(
          title = "Hello World!",
          scene = new Scene(
            new StackPane() {
              getChildren.add(
                new BorderPane().set(
                  bottom = new Button().set(
                    text = "Say 'Hello World'",
                    onAction = {
                      println("Hello World!")
                    }
                  ),
                  center = slider,
                  top = new Label().set(
                    text = bind(s"Slider is at ${slider.getValue.toInt}")
                  )
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
    
The syntactic facilities available so far are:
- JavaFX Script-like syntax for setters (without any runtime penalty or loss of type-safetiness): 

        button.set(
          text = "Say 'Hello World'",
          tooltip = new Tooltip("Hover me"),
          height = 300,
          width = 200
        )
      
- Simpler syntax for event handlers, with or without the event parameter:
    
        button1.set(
          text = "Click me!",
          onAction = println("clicked")
        )
        
        button2.set(
          text = "Click me!",
          onAction = (event: ActionEvent) => {
            println(s"clicked: $event")
          }
        )
        
        button2.maxWidthProperty onChange {
          println("Constraint changed!")
        }
        
  Instead of:
  
        button3.setText("Click me!")
        button3.setOnAction(new EventHandler[ActionEvent]() {
          override def handle(event: ActionEvent) {
            println(s"clicked: $event")
          }
        }
        button3.maxWidthProperty.addListener(new ChangeListener[Double]() {
          override def changed(observable: ObservableValue[Double], oldValue: Double, newValue: Double) {
            println("Something happend
          }
        }
        
- More natural bindings:

        {
          val moo: ObservableDoubleValue = ...
          val foo = bind {
            Math.sqrt(moo.getValue)
          }
        }
        
  Instead of:
  
        {
          val moo: ObservableDoubleValue = ...
          val foo = new DoubleBinding() {
            super.bind(moo)
            override def computeValue() = 
              Math.sqrt(moo.getValue)
          }
        }
