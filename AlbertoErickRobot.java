package alberto;

import robocode.*;

import java.util.*;
import java.awt.*;
import java.util.List;
import org.jpl7.Query;
import org.jpl7.Term;

// Feito por Alberto Oliveira Santos e Erick Silva Kokubum

public class AlbertoErickRobot extends AdvancedRobot {
    private static final int HIT_NUMBERS_TO_SWITCH_MOVEMENT = 3;
    private static final long HIT_EVALUATE_INTERVAL = 3000; // 3 segundos em milissegundos
    private static final long HIT_WALL_EVALUATE_INTERVAL = 6000; // 6 segundos em milissegundos
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
    	if (!Query.hasSolution("consult('script.pl').")){
    		System.out.println("Consult failed");
    	}
    	
        setup();

        while(true) {
        	Query evaluatHitIntervalQ = new Query("evaluateHitInterval",new Term[] {
	           new org.jpl7.Integer(getTime()),
	           new org.jpl7.Integer(firstHitTime),
	           new org.jpl7.Integer(HIT_EVALUATE_INTERVAL)
        	});
            if (evaluatHitIntervalQ.hasSolution()) {
                System.out.println("Limite de tempo ultrapassado - zerando hits");

                numberOfHits = 0;
            }
            System.out.println("move strategy");
            doMoveStrategy();

            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    private void setup() {
        setColors(Color.red, Color.blue, Color.white);
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
        Query oneHitQ = new Query("numberOfHitsIsOne",new Term[] {
           new org.jpl7.Integer(numberOfHits)
        });
        if(oneHitQ.hasSolution()) {
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
        Query oneHitQ = new Query("numberOfHitsIsOne",new Term[] {
           new org.jpl7.Integer(hitWallCount)
        });
        if(oneHitQ.hasSolution()) {
            System.out.println("Primeiro HIT Wall");

            firstHitWallTime = getTime();
        }
        
        Query hitWallCheckQ = new Query("hitWallCountAndTimeCheck",new Term[] {
                new org.jpl7.Integer(hitWallCount),
                new org.jpl7.Integer(getTime()),
                new org.jpl7.Integer(firstHitWallTime),
                new org.jpl7.Integer(HIT_NUMBERS_TO_SWITCH_MOVEMENT),
                new org.jpl7.Integer(HIT_WALL_EVALUATE_INTERVAL)
        });
        


        if (hitWallCheckQ.hasSolution()) {
            System.out.println("Too much wall hit, changing to circle");
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
    	Query zeroVelocityQ = new Query("velocityIsZero",new Term[] {
    		new org.jpl7.Float(getVelocity())
    	});
        setMaxVelocity(rand.nextDouble() * 8 + 2); // Velocidade aleatória entre 2 e 10
        if (zeroVelocityQ.hasSolution()) {
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
        Query mod20Q = new Query("timeModulo20",new Term[] {
        		new org.jpl7.Integer(getTime())
        });

        if (mod20Q.hasSolution()) {
            directionMovementSign *= -1;
            setAhead(150 * directionMovementSign);
            execute();
            System.out.println("moving");

        }
    }

    double setNormalizeAngleForBearing(double angle) {
    	Query greaterQ = new Query("angleGreaterThan180",new Term[] {
        		new org.jpl7.Float(angle)
        });
    	Query lessQ = new Query("angleLessThanMinus180",new Term[] {
        		new org.jpl7.Float(angle)
        });


        while (greaterQ.hasSolution()) {
            angle -= 360;
        }
        while (lessQ.hasSolution()) {
            angle += 360;
        }
        return angle;
    }

    void doShootStrategy(ScannedRobotEvent e) {
    	Query zeroVelocityQ = new Query("velocityIsZero",new Term[] {
        		new org.jpl7.Float(e.getVelocity())
        });
        System.out.println("Velocity: " + e.getVelocity());
        System.out.println("Heading: " + e.getHeading());

        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        if (zeroVelocityQ.hasSolution()) {
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