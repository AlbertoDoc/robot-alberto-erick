package alberto;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;

import java.awt.*;

public class AlbertoErickRobot extends AdvancedRobot {

    boolean isInAttackMovement = false;

    WallNear wall = WallNear.NONE;
    Boolean isAvoidingWall = false;
    public void run() {
		setup();

        while(true) {
            // Replace the next 4 lines with any behavior you would like
            System.out.print("x: ");
            System.out.println(getX());
            System.out.print("y: ");
            System.out.println(getY());
            wall = isNearWall();
            System.out.println("proximo a parede: ");

            if (!isInAttackMovement) {
                turnRadarLeft(90);
            }

            switch (wall) {
                case LEFT_WALL:
                    System.out.println("esquerda");
                    if (!isAvoidingWall) {
                        stop();
                        turnRight(90);
                        ahead(100);
                    }
                    isAvoidingWall = true;
                    break;
                case RIGHT_WALL:
                    System.out.println("direita");
                    if (!isAvoidingWall) {
                        stop();
                        turnRight(90);
                        ahead(100);
                    }
                    isAvoidingWall = true;
                    break;
                case BOTTOM_WALL:
                    System.out.println("abaixo");
                    if (!isAvoidingWall) {
                        stop();
                        turnRight(90);
                        ahead(100);
                    }
                    isAvoidingWall = true;
                    break;
                case TOP_WALL:
                    System.out.println("acima");
                    if (!isAvoidingWall) {
                        stop();
                        turnRight(90);
                        ahead(100);
                        isAvoidingWall = true;
                    }
                    break;
                case NONE:
                    if (!isInAttackMovement) {
                        ahead(30);
                    }
                    isAvoidingWall = false;
                    break;
            }
        }
    }
	
	private void setup() {
		setColors(Color.red, Color.blue, Color.white);
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

    /**
     * onScannedRobot: What to do when you see another robot
     */
    public void onScannedRobot(ScannedRobotEvent e) {
        System.out.println("robo escaneado!");
        isInAttackMovement = true;

        // vira o robo a 90 graus do alvo scaneado
        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(absoluteBearing -
                getGunHeadingRadians() + 1.57));
        ahead(100);
        back(100);
        isInAttackMovement = false;
    }

    /**
     * onHitByBullet: What to do when you're hit by a bullet
     */
    public void onHitByBullet(HitByBulletEvent e) {
        //back(10);
    }

    /**
     * onHitWall: What to do when you hit a wall
     */
    public void onHitWall(HitWallEvent e) {
        System.out.println("bateu na parede");
    }

    enum WallNear {
        NONE, LEFT_WALL, RIGHT_WALL, TOP_WALL, BOTTOM_WALL,
        LEFT_BOTTOM_WALL, LEFT_TOP_WALL, RIGHT_TOP_WALL, RIGHT_BOTTOM_WALL
    }
}
