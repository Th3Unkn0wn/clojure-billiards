(ns billiards.gui.actions
  (:use
    [seesaw mouse]
    [billiards.constants]
    [billiards.state.global]
    [billiards.state.board]
    [billiards.state.gui]
    [billiards.state.initial :only [reset-game]]
    [billiards.physics.geometry]
    [billiards.logic.main :only [turn]]
    [billiards.utilities]))

(def is-shooting (atom false))

(defn change-power [change]
  (swap! cue-power (fn [old]
                     (let [new-val (+ old change)]
                       (cond
                         (< new-val 0) 0
                         (> new-val 100) 100
                         :else new-val)))))

(defn shoot []
  (reset! is-playing false)
  (let [ball (get-white-ball)
        dir (get-vect-from-angle @cue-angle)
        [dir-x dir-y] (normalize-vect dir)]
    (dosync
      (alter ball update-in [:speed] (fn [old] (/ (* ball-max-power @cue-power) 100)))
      (alter ball update-in [:dir-x] (fn [old] dir-x))
      (alter ball update-in [:dir-y] (fn [old] dir-y))))
  (turn)
  (when-not (zero? @game-ended)
    (seesaw.core/alert (format "Player %d won!", (if (= @game-ended 1) 1 2)))
    (reset-game)))

(defn start-shooting []
  (reset! is-shooting true)
  (while @is-shooting
    (change-power 1)
    (Thread/sleep 15))
  (reset! cue-power 0))

(defn place-free-ball []
  (let [white-ball (get-white-ball)]
    (when (every? #(= % false) (for [ball @balls
                                     :when (not= ball white-ball)]
                                 (circle-collision-circle?
                                   [(:x @white-ball) (:y @white-ball) ball-size]
                                   [(:x @ball) (:y @ball) ball-size])))
      (reset! is-free-ball false))))

(defn left-button-clicked []
  (when @is-playing
    (if @is-free-ball
      (place-free-ball)
      (do
        (if @is-shooting
          (do
            (shoot)
            (reset! is-shooting false))
          (start-shooting))))))

(defn right-button-clicked []
  (when (and @is-playing @is-shooting)
    (reset! is-shooting false)
    (reset! cue-power 0)))

(defn mouse-released [event]
  (let [button (.getButton event)]
    (case button
      1 (left-button-clicked)
      3 (right-button-clicked))))

(defn adjust-cue [mousex mousey white-ball]
  (let [dir (subtract-pair [mousex mousey] [(+ board-start-x (:x @white-ball)) (+ board-start-y (:y @white-ball))])]
    (reset! cue-angle (get-angle-from-vect dir))))

(defn adjust-ball [mousex mousey white-ball]
  (dosync
    (alter white-ball update-in [:x] (fn [x]
                                       (- (coerce-number-in-range
                                            mousex
                                            (+ (* 2 ball-size) board-padding board-start-x)
                                            (+ board-start-x (- board-width (* 2 ball-size) board-padding))) board-start-x)))
    (alter white-ball update-in [:y] (fn [x]
                                       (- (coerce-number-in-range
                                            mousey
                                            (+ (* 2 ball-size) board-padding board-start-y)
                                            (+ board-start-y (- board-height (* 2 ball-size) board-padding))) board-start-y)))))
(defn mouse-moved [event]
  (when @is-playing
    (let [[mousex mousey] (location event)
          [mousex mousey] [(- mousex 10) (- mousey 30)]
          white-ball (get-white-ball)]
      (if @is-free-ball
        (adjust-ball mousex mousey white-ball)
        (adjust-cue mousex mousey white-ball)))))
