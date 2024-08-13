package albertoErick;

import robocode.*;

import java.util.*;
import java.awt.*;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;

// Feito por Alberto Oliveira Santos e Erick Silva Kokubum

public class AlbertoErickRobot extends AdvancedRobot {
    private BayesNet net;
    private IBayesInferer inferrer;
    private double enemyDistance;


    private static final int HIT_NUMBERS_TO_SWITCH_MOVEMENT = 3;
    private static final long HIT_EVALUATE_INTERVAL = 3000; // 3 segundos em milissegundos
    private static final long HIT_WALL_EVALUATE_INTERVAL = 6000; // 6 segundos em milissegundos
    private static final double ENEMY_THRESHOLD_DISTANCE = 70.0; // distancia limit bayes do inimigo
    private static final long HIT_TOO_MUCH_EVALUATE_INTERVAL = 5000; // 5 segundos em milissegundos
    private static final int V_LOWER_LIMIT_SLOW = 2;
    private static final int V_LOWER_LIMIT_MEDIUM = 4;
    private static final int V_LOWER_LIMIT_FAST = 6;

    int numberOfHits = 0;
    long firstHitTime = 0;
    long hitTooMuchTime = 0;

    private byte directionMovementSign = 1;
    double enemyBearing = 0;
    int hitWallCount = 0;
    long firstHitWallTime = 0;
    Random rand = new Random();
    MoveStrategy moveStrategy = MoveStrategy.ZIG_ZAG;
    double enemyLastAbsBearing = 0.0;
    boolean hitTooMuch = false;

    final static String fclFilePath = "./albertoErickFcl.fcl";
    private FunctionBlock firePower;
    private FunctionBlock prediction;
    private FIS fis = null;

    public void run() {
        // Initialize fuzzy
        fis = FIS.load(fclFilePath, true);
        firePower = fis.getFunctionBlock("firePower");
        prediction = fis.getFunctionBlock("prediction");

        setup();
        // Initialize the Bayesian network
        initializeBayesianNetwork();

        while(true) {
            if (getTime() - firstHitTime > HIT_EVALUATE_INTERVAL) {
                System.out.println("Limite de tempo ultrapassado - zerando hits");

                numberOfHits = 0;
            }
            System.out.println("move strategy");

            doMoveStrategy();

            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    private void initializeBayesianNetwork() {
        net = new BayesNet();
        System.out.println("Criando node MS -> " + MoveStrategy.ZIG_ZAG.toString() + " | " + MoveStrategy.CIRCLE.toString());

        BayesNode enemyDistance = net.createNode("ED");
        enemyDistance.addOutcomes("CLOSE", "FAR");
        enemyDistance.setProbabilities(0.3, 0.7);

        BayesNode wallDistance = net.createNode("WD");
        wallDistance.addOutcomes("CLOSE", "FAR");
        wallDistance.setProbabilities(0.4, 0.6);

        BayesNode movementStrategy = net.createNode("MS");
        movementStrategy.addOutcomes(MoveStrategy.ZIG_ZAG.toString(), MoveStrategy.CIRCLE.toString());
        movementStrategy.setParents(Arrays.asList(enemyDistance, wallDistance));
        movementStrategy.setProbabilities(
                // ED - CLOSE
                0.2, 0.8,	// WD - CLOSE
                0.4, 0.6, 	// WD - FAR
                // ED - FAR
                0.1, 0.9,	// WD - CLOSE
                0.5, 0.5 	// WD - FAR
        );

        BayesNode velocityLowerLimit = net.createNode("V");
        velocityLowerLimit.addOutcomes("SLOW", "NORMAL", "FAST");
        velocityLowerLimit.setParents(Arrays.asList(enemyDistance));
        velocityLowerLimit.setProbabilities(
                0.1, 0.1, 0.8,		// ED - CLOSE
                0.25, 0.5, 0.25 	// ED - FAR
        );


        inferrer = new JunctionTreeAlgorithm();
        inferrer.setNetwork(net);
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
        enemyDistance = e.getDistance();

        if ((System.currentTimeMillis() - hitTooMuchTime) > HIT_TOO_MUCH_EVALUATE_INTERVAL) {
            System.out.println("Resetando hit too much");

            hitTooMuch = false;
        }
    }

    /**
     * onHitByBullet: What to do when you're hit by a bullet
     */
    public void onHitByBullet(HitByBulletEvent e) {
        numberOfHits++;
        if(numberOfHits == 1) {
            System.out.println("Primeiro HIT");

            firstHitTime = System.currentTimeMillis();
        }

        if(numberOfHits >= HIT_NUMBERS_TO_SWITCH_MOVEMENT && (System.currentTimeMillis() - firstHitTime <= HIT_EVALUATE_INTERVAL)){
            System.out.println("Mudando estratégia");
            hitTooMuch = true;
            hitTooMuchTime = System.currentTimeMillis();

            if(moveStrategy == MoveStrategy.ZIG_ZAG) {
                moveStrategy = MoveStrategy.CIRCLE;
            }else {
                moveStrategy = MoveStrategy.ZIG_ZAG;

            }
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
            System.out.println("Too much wall hit, changing to circle");
            hitWallCount = 0;
            hitTooMuch = true;
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

    private WallNear isNearWall() {
        if (getX() < 60.0) {
            return WallNear.LEFT_WALL;
        }

        if (getX() > 760.0) {
            return WallNear.RIGHT_WALL;
        }

        if (getY() < 60.0) {
            return WallNear.BOTTOM_WALL;
        }

        if (getY() > 560.0) {
            return WallNear.TOP_WALL;
        }

        return WallNear.NONE;
    }

    public void doMoveStrategy() {
        // Set evidence for Bayesian network
        Map<BayesNode,String> evidence = new HashMap<BayesNode,String>();
        System.out.println("proximo a parede: "+ isNearWall().toString());
        evidence.put(net.getNode("WD"), isNearWall() == WallNear.NONE ? "FAR" : "CLOSE");
        evidence.put(net.getNode("ED"), enemyDistance > ENEMY_THRESHOLD_DISTANCE ? "FAR" : "CLOSE");

        inferrer.setEvidence(evidence);

        double[] beliefsMS = inferrer.getBeliefs(net.getNode("MS"));

        if (!hitTooMuch) {
            if (beliefsMS[0] >= beliefsMS[1]) {
                moveStrategy = MoveStrategy.ZIG_ZAG;
            }else if (beliefsMS[1] > beliefsMS[0]) {
                moveStrategy = MoveStrategy.CIRCLE;
            }
        }

        double[] beliefsV = inferrer.getBeliefs(net.getNode("V"));

        int vLowerLimit = getVLowerLimit(beliefsV[0],beliefsV[1],beliefsV[2]);
        System.out.println(vLowerLimit + " LOWER LIMIT V");

        switch (moveStrategy) {
            case ZIG_ZAG:
                System.out.println("Começando ZIG ZAG");

                doMoveInZigZag(vLowerLimit);
                break;
            case CIRCLE:
                System.out.println("Começando CIRCLE");

                doMoveInCircle(vLowerLimit);
                break;
        }
    }

    public int getVLowerLimit(double slow, double medium, double fast) {
        if(slow >= medium && slow >= fast) {
            return V_LOWER_LIMIT_SLOW;
        }

        if(medium >= slow && medium >= fast) {
            return V_LOWER_LIMIT_MEDIUM;
        }

        if(fast >= slow && fast >= medium) {
            return V_LOWER_LIMIT_FAST;
        }

        return V_LOWER_LIMIT_SLOW;
    }

    public void doMoveInCircle(int vLowerLimit) {
        setMaxVelocity(rand.nextDouble() * 8 + vLowerLimit); // Velocidade aleatória entre lower limit e 10 + lower limit
        if (isNearWall() != WallNear.NONE || getVelocity() == 0) {
            directionMovementSign *= -1;
        }
        setTurnRight(setNormalizeAngleForBearing(enemyBearing + 90));
        setAhead(1000 * directionMovementSign);

        execute();
    }




    public void doMoveInZigZag(int vLowerLimit) {
        setMaxVelocity(rand.nextDouble() * 10 + vLowerLimit); // Velocidade aleatória entre lower limit e 10 + lower limit

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
        double futureBearing = Math.abs(absoluteBearing - enemyLastAbsBearing);
        if (e.getVelocity() == 0.0) {
            setTurnGunRightRadians(
                    robocode.util.Utils.normalRelativeAngle(absoluteBearing -
                            getGunHeadingRadians()));
        } else {
            prediction.setVariable("enemySpeed", e.getVelocity());
            prediction.setVariable("distance", e.getDistance());
            prediction.setVariable("aimCorrection", futureBearing);

            prediction.evaluate();

            double result = prediction.getVariable("prediction").getValue();
            System.out.println("predictionResult: " + result);
            setTurnGunRightRadians(
                    robocode.util.Utils.normalRelativeAngle((absoluteBearing + result) -
                            getGunHeadingRadians()));
        }

        System.out.println("aimCorrection: " + futureBearing);

        fire(calculateBulletPower(getEnergy(), e.getDistance(), futureBearing, e.getVelocity()));
        enemyLastAbsBearing = absoluteBearing;
    }

    double calculateBulletPower(double ourEnergy, double distance, double prediction, double enemySpeed) {
        firePower.setVariable("ourEnergy", ourEnergy);
        firePower.setVariable("distance", distance);
        firePower.setVariable("prediction", prediction);
        firePower.setVariable("enemySpeed", Math.abs(enemySpeed));

        System.out.println("ourEnergy" + ourEnergy);
        System.out.println("distance" + distance);
        System.out.println("prediction" + prediction);
        System.out.println("enemySpeed" + Math.abs(enemySpeed));

        firePower.evaluate();

        double result = firePower.getVariable("firePower").getValue();
        System.out.println("poder tiro: " + result);

        return result;
    }
}
