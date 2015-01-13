(defproject hello-world "1.0.0"
  :description "Simple Sokoban clone"
  
  :dependencies [[com.badlogicgames.gdx/gdx "1.5.0"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl "1.5.0"]
                 [com.badlogicgames.gdx/gdx-box2d "1.5.0"]
                 [com.badlogicgames.gdx/gdx-box2d-platform "1.5.0"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-bullet "1.5.0"]
                 [com.badlogicgames.gdx/gdx-bullet-platform "1.5.0"
                  :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-platform "1.5.0"
                  :classifier "natives-desktop"]
                 [org.clojure/clojure "1.6.0"]
                 [play-clj "0.4.3"]]
  
  :source-paths ["src" "src-common"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :aot [hello-world.core.desktop-launcher]
  :main hello-world.core.desktop-launcher)
