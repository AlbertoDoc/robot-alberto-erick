package alberto;

import robocode.*;

import java.util.*;
import java.awt.*;
import java.util.List;

// Feito por Alberto Oliveira Santos e Erick Silva Kokubum

public class AlbertoErickRobot extends AdvancedRobot {
    private static final int HIT_NUMBERS_TO_SWITCH_MOVEMENT = 3;
    private static final long HIT_EVALUATE_INTERVAL = 3000; // 3 segundos em milissegundos
    private static final long HIT_WALL_EVALUATE_INTERVAL = 6000; // 3 segundos em milissegundos
    int numberOfHits = 0;
    long firstHitTime = 0;
    private byte directionMovementSign = 1;
    double enemyBearing = 0;
    int hitWallCount = 0;
    long firstHitWallTime = 0;
    Random rand = new Random();
    MoveStrategy moveStrategy = MoveStrategy.ZIG_ZAG;
    List<MoveStrategy> moveStrategyList = Arrays.asList(MoveStrategy.ZIG_ZAG, MoveStrategy.CIRCLE);
    int strategyIndex = 0;
    double enemyLastAbsBearing = 0.0;

    public void run() {
        setup();

        while(true) {

            if (getTime() - firstHitTime > HIT_EVALUATE_INTERVAL) {
                System.out.println("Limite de tempo ultrapassado - zerando hits");

                numberOfHits = 0;
            }
            doMoveStrategy();

            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    private void setup() {
        setColors(Color.red, Color.blue, Color.white);
    }

    private WallNear isNearWall() {
        if (getX() < 80.0) {
            return WallNear.LEFT_WALL;
        }

        if (getX() > 740.0) {
            return WallNear.RIGHT_WALL;
        }

        if (getY() < 80.0) {
            return WallNear.BOTTOM_WALL;
        }

        if (getY() > 540.0) {
            return WallNear.TOP_WALL;
        }

        return WallNear.NONE;
    }

    /**
     * onScannedRobot: What to do when you see another robot
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        enemyBearing = e.getBearing();
        doMoveStrategy();
        doShootStrategy(e);
    }

    /**
     * onHitByBullet: What to do when you're hit by a bullet
     */
    public void onHitByBullet(HitByBulletEvent e) {
        numberOfHits++;
        if(numberOfHits == 1) {
            System.out.println("Primeiro HIT");

            firstHitTime = getTime();
        }

        if(numberOfHits >= HIT_NUMBERS_TO_SWITCH_MOVEMENT && (getTime() - firstHitTime <= HIT_EVALUATE_INTERVAL)){
            System.out.println("Mudando estratégia");

            strategyIndex++;
            int expectedIndex = strategyIndex % moveStrategyList.size();
            moveStrategy = moveStrategyList.get(expectedIndex);
            numberOfHits = 0;
        }

    }

    /**
     * onHitWall: What to do when you hit a wall
     */
    public void onHitWall(HitWallEvent e) {
        System.out.println("HIT Wall");

        hitWallCount++;
        if(numberOfHits == 1) {
            System.out.println("Primeiro HIT Wall");

            firstHitWallTime = getTime();
        }
        if (hitWallCount >= HIT_NUMBERS_TO_SWITCH_MOVEMENT && (getTime() - firstHitWallTime <= HIT_WALL_EVALUATE_INTERVAL)) {
            hitWallCount = 0;
            moveStrategy = MoveStrategy.CIRCLE;
        }
    }

    enum WallNear {
        NONE, LEFT_WALL, RIGHT_WALL, TOP_WALL, BOTTOM_WALL,
        LEFT_BOTTOM_WALL, LEFT_TOP_WALL, RIGHT_TOP_WALL, RIGHT_BOTTOM_WALL
    }
    enum MoveStrategy {
        ZIG_ZAG, CIRCLE
    }

    public void doMoveStrategy() {
        switch (moveStrategy) {
            case ZIG_ZAG:
                System.out.println("Começando ZIG ZAG");

                doMoveInZigZag();
                break;
            case CIRCLE:
                System.out.println("Começando CIRCLE");

                doMoveInCircle();
                break;
        }
    }

    public void doMoveInCircle() {
        setMaxVelocity(rand.nextDouble() * 8 + 2); // Velocidade aleatória entre 2 e 10
        if (isNearWall() != WallNear.NONE || getVelocity() == 0) {
            directionMovementSign *= -1;
        }
        setTurnRight(setNormalizeAngleForBearing(enemyBearing + 90));
        setAhead(1000 * directionMovementSign);

        execute();
    }


    public void doMoveInZigZag() {
        setMaxVelocity(rand.nextDouble() * 8 + 2); // Velocidade aleatória entre 2 e 10

        setTurnRight(setNormalizeAngleForBearing(enemyBearing + 90));
        execute();

        System.out.println(getTime() % 20);

        if (getTime() % 20 == 0) {
            directionMovementSign *= -1;
            setAhead(150 * directionMovementSign);
            execute();
            System.out.println("moving");

        }
    }

    double setNormalizeAngleForBearing(double angle) {

        while (angle >  180) {
            angle -= 360;
        }
        while (angle < -180) {
            angle += 360;
        }
        return angle;
    }

    void doShootStrategy(ScannedRobotEvent e) {
        System.out.println("Velocity: " + e.getVelocity());
        System.out.println("Heading: " + e.getHeading());

        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        if (e.getVelocity() == 0.0) {
            setTurnGunRightRadians(
                    robocode.util.Utils.normalRelativeAngle(absoluteBearing -
                            getGunHeadingRadians()));
        } else {
            double futureBearing = Math.abs(absoluteBearing - enemyLastAbsBearing);
            setTurnGunRightRadians(
                    robocode.util.Utils.normalRelativeAngle((absoluteBearing + futureBearing) -
                            getGunHeadingRadians()));
        }

        fire(calculateBulletPower(e.getDistance()));
        enemyLastAbsBearing = absoluteBearing;
    }

    double calculateBulletPower(double distance) {
        return Math.max(3.0 / (distance / 100.0), 0.5);
    }
}
