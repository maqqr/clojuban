(ns hello-world.core
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer :all]))

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

; http://www.reddit.com/r/playclj/comments/2oq1wg/passing_game_state_is_the_entities_vector_the/


; Constants.

(def ^:const tile-width 64)
(def ^:const tile-height 32)

(def ^:const hw (/ tile-width 2))
(def ^:const hh (/ tile-height 2))

(def ^:const board-width 8)
(def ^:const board-height 9)


; Settings.

(def keybindings {
  (key-code :dpad-up) :up
  (key-code :dpad-down) :down
  (key-code :dpad-left) :left
  (key-code :dpad-right) :right
  })


; Helper functions for isometric coordinates.

(defn to-isom-x [xx yy] (+ 300 (* hw (- xx yy))))

(defn to-isom-y [xx yy] (+ 300 (* hh (+ xx yy))))

(defn recalc-pos
  "Re-calculates every entity's :x and :y using :iso-x and :iso-y."
  [ents]
  (map (fn [ent] (assoc ent
    :x (to-isom-x (:iso-x ent) (:iso-y ent))
    :y (to-isom-y (:iso-x ent) (:iso-y ent))))
  ents))

(defn depth-sort
  "Sorts entities by isometric depth."
  [ents]
  (sort-by (fn [obj]
    (* -1 (+ (:iso-x obj) (:iso-y obj))))
  ents))


; Game object creation.

(defn make-walls
  "Creates walls.
  'walls' is a vector of coordinate pairs."
  [walls]
  (mapv
    (fn [pos] (assoc (texture "wall.png")
      :id :wall
      :x (to-isom-x (first pos) (second pos))
      :y (to-isom-y (first pos) (second pos))
      :iso-x (first pos)
      :iso-y (second pos)
      :width 64 :height 128 :origin-x 32 :origin-y 128))
    walls))

(defn make-floor
  "Creates a list of floor tiles."
  [width height marks]
  (for [xx (range width) yy (range height)]
    (assoc (texture (if (some #{[xx yy]} marks) "floormark.png" "floor.png"))
      :id :floor
      :x (to-isom-x xx yy)
      :y (to-isom-y xx yy)
      :iso-x xx
      :iso-y yy
      :width 64 :height 32 :origin-x 32 :origin-y 32)))

(defn make-box
  "Creates a box at (xx, yy)."
  [xx yy]
  (assoc (texture "box.png")
    :id :box
    :x (to-isom-x xx yy)
    :y (to-isom-y xx yy)
    :iso-x xx
    :iso-y yy
    :width 64 :height 128 :x-origin 32 :y-origin 128))

(defn make-player
  "Creates player at (xx, yy)."
  [xx yy]
  (assoc (texture "player.png")
    :id :player
    :x (to-isom-x xx yy)
    :y (to-isom-y xx yy)
    :iso-x xx
    :iso-y yy
    :width 64 :height 128 :x-origin 32 :y-origin 128))


; Game object movement and helper functions.

(defn move
  "Moves any entity that has iso-x and iso-y keywords.
  'dir' is a direction defined in 'keybindings' hash-map."
  [ent dir]
  (case dir
    :down  (assoc ent :iso-y (dec (:iso-y ent)))
    :up    (assoc ent :iso-y (inc (:iso-y ent)))
    :left  (assoc ent :iso-x (dec (:iso-x ent)))
    :right (assoc ent :iso-x (inc (:iso-x ent)))
    ent))

(defn updater
  "Updates all entities that have specific id using 'func' function."
  [ents id func]
  (map (fn [ent]
    (if (= (:id ent) id) (func ent) ent))
  ents))

(defn filter-id
  "Filters entities by id keyword."
  [id ents]
  (filter (fn [ent] (= (:id ent) id)) ents))

(defn same-pos
  "Returns true if 'ent1' and 'ent2' are on the same tile."
  [ent1 ent2]
  (and (= (:iso-x ent1) (:iso-x ent2)) (= (:iso-y ent1) (:iso-y ent2))))

(defn box-mover
  "Moves any box that is in the same tile with player."
  [dir ents]
  (let [player (first (filter-id :player ents))]
    (map
      (fn [ent]
        ; if a box is where the player is, move the box.
        (if (and (= (:id ent) :box) (same-pos ent player))
          (move ent dir)
          ent)
      )
      ents)))

(defn move-player
  "Moves player entity and player moves boxes."
  [ents dir]
  (box-mover dir (updater ents :player (fn [pl] (move pl dir)))))


; State validator.

(defn valid-state? [ents]
  "Returns true if entities are in valid positions."
  (let [positions (map (fn [ent] [(:iso-x ent) (:iso-y ent)]) ents)]
    (and
      ; All entities must be in distinct positions.
      (= (count (distinct positions)) (count positions))
      ; All entities must be within game borders.
      (not-any?
        (fn [p] (let [xx (first p) yy (second p)]
          (or
            (> 0 xx)
            (> 0 yy)
            (<= board-width xx)
            (<= board-height yy))))
        positions))))


; Screens.

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen
      :renderer (stage)
      :camera (orthographic)
      :floor (make-floor
        board-width
        board-height
        (list [1 2] [1 4] [5 3] [4 5])))
    (let [walls (list [2 0] [3 0] [4 0] [5 0] [6 0]
                      [0 1] [1 1] [2 1] [6 1]
                      [0 2] [6 2]
                      [0 3] [1 3] [2 3] [6 3]
                      [0 4] [2 4] [3 4] [6 4]
                      [0 5] [2 5] [6 5] [7 5]
                      [0 6] [7 6]
                      [0 7] [7 7]
                      [0 8] [1 8] [2 8] [3 8] [4 8] [5 8] [6 8] [7 8])]
      [(make-walls walls) (make-player 2 2) (make-box 3 2) (make-box 4 3) (make-box 4 4) (make-box 1 6)]))

  :on-resize
  (fn [screen entities]
    (height! screen 600))

  :on-render
  (fn [screen entities]
    (clear!)
    ; Render floor first, then other entities.
    (render! screen (depth-sort (screen :floor)))
    (render! screen (depth-sort (recalc-pos entities)))
  )

  :on-key-down
  (fn [screen entities]
    (let [new-entities (move-player entities (get keybindings (:key screen)))]
      (if (valid-state? new-entities) new-entities entities))))


(defgame hello-world
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
