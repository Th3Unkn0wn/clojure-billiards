(ns billiards.physics.collisions
  (:use
    [billiards.state.board :only [borders]]
    [billiards.physics.geometry]
    [billiards.physics.ball_physics]
    [billiards.constants]))

(defn collision-border-ball-check? [ball border]
  (segment-collision-circle? (:start border) (:end border) [[(:x @ball) (:y @ball)] ball-size]))

(defn collision-borders-ball-check? [ball]
  (some #{true} (for [border @borders]
                  (collision-border-ball-check? ball border))))

(defn right-border? [ball [new-dirx new-diry]]
  (dosync
    (alter ball update-in [:x] #(+ % new-dirx))
    (alter ball update-in [:y] #(+ % new-diry)))
  (let [result (collision-borders-ball-check? ball)]
    (dosync
      (alter ball update-in [:x] #(- % new-dirx))
      (alter ball update-in [:y] #(- % new-diry)))
    (not result)))

(defn collision-border-ball [ball border]
  (when (collision-border-ball-check? ball border)
    (let [new-dir (reflect-vector-from-normal [(:dirx @ball) (:diry @ball)] (:normal border))
          [reverse-dir-x reverse-dir-y] (product-vector-scalar [(:dirx @ball) (:diry @ball)] -1)]
      (while (collision-borders-ball-check? ball)
        (dosync
          (alter ball update-in [:x] #(+ % reverse-dir-x))
          (alter ball update-in [:y] #(+ % reverse-dir-y))))
      (if (right-border? ball new-dir)
        (do
          (apply-direction ball new-dir (* (:speed @ball) cushion-effect))
          true)
        (dosync
          (alter ball update-in [:x] #(+ % (:dirx @ball)))
          (alter ball update-in [:y] #(+ % (:diry @ball)))
          false)))))

(defn collision-borders-ball? [ball]
  (let [break-token (atom false)]
    (doseq [border @borders]
      (when (not @break-token)
        (reset! break-token (collision-border-ball ball border))))
    @break-token))

(defn collision-ball-ball? [[ball1 ball2]]
  (when (circle-collision-circle? [(:x @ball1) (:y @ball1) ball-size] [(:x @ball2) (:y @ball2) ball-size])
    (fix-position ball1 ball2)
    (calculate-new-direction ball2 ball1)
    true))

(defn collision-ball-pocket? [ball pocket]
  (let [distance (distance-point-to-point [(:x @ball) (:y @ball)] pocket)]
    (<= distance pocket-size)))