package movement;

import core.Coord;
import core.Settings;

public class LevyWalkMovement extends MovementModel {

     /**
     * @author Trustacean
     */
    private Coord lastWaypoint;
    private double alpha; // Ditambahkan: parameter alpha

    public LevyWalkMovement(Settings settings) {
        super(settings);
        this.alpha = settings.getDouble("alpha"); // Ambil dari config, default 0.5
    }

    public LevyWalkMovement(LevyWalkMovement lw) {
        super(lw);
        this.alpha = lw.alpha; // Salin nilai alpha dari objek lama
    }

    @Override
    public Path getPath() {
        Path p = new Path(generateSpeed());
        p.addWaypoint(lastWaypoint.clone());
        Coord nextWaypoint = null;

        int attempt = 0;

        while (true) {
            double stepLength = getRandomPareto(alpha) - (attempt * 0.1); // Gunakan field alpha
            double theta = rng.nextDouble() * 2 * Math.PI;

            double newX = lastWaypoint.getX() + stepLength * Math.cos(theta);
            double newY = lastWaypoint.getY() + stepLength * Math.sin(theta);

            nextWaypoint = new Coord(newX, newY);

            if (newX > 0 && newY > 0 && newX < getMaxX() && newY < getMaxY()) {
                break;
            }

            attempt++;
        }

        p.addWaypoint(nextWaypoint);
        lastWaypoint = nextWaypoint;

        return p;
    }

    @Override
    public Coord getInitialLocation() {
        assert rng != null : "MovementModel not initialized!";
        Coord c = new Coord(rng.nextDouble() * getMaxX(), rng.nextDouble() * getMaxY());

        this.lastWaypoint = c;
        return c;
    }

    @Override
    public MovementModel replicate() {
        return new LevyWalkMovement(this);
    }

    private double getRandomPareto(double alpha) {
        double u = rng.nextDouble();
        return Math.pow(1.0 - u, -1.0 / alpha);
    }
}
