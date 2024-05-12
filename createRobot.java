package albertoErick;

import javax.tools.*;
import java.io.*;
/* 
 * Create Robot
 * Creates and compiles Robocode Java file to test 
 * 
 */

public class createRobot {

	public static void create(double[] chromo) {
		createRobotFile(chromo); // create file
		compile(); // now compile it
	}
	
	public static void compile () {
		String fileToCompile = "albertoErick/AlbertoErickRobotGA.java"; // which file to compile * rhyming :) *
	    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	    compiler.run(null, null, null, fileToCompile); // run compile
	}
	
	public static void createRobotFile(double[] chromo){
		try {
			FileWriter fstream = new FileWriter("albertoErick/AlbertoErickRobotGA.java"); // file name to create
			BufferedWriter out = new BufferedWriter(fstream);
			
			//start code
			out.write("package albertoErick; \n " +
				"import robocode.*; \n" +
				"import robocode.Robot;\n" +
				"import java.awt.Color;\n" +
				"import java.util.*;\n" +
				"import java.awt.*;\n" +
				"import java.util.List;\n");
			
					// build up robot logic in here
					// access chromosomes from array to set as variables
					
					// end of robot
					out.append("public class AlbertoErickRobotGA extends AdvancedRobot {\n");
					out.append("private static final int HIT_NUMBERS_TO_SWITCH_MOVEMENT = " + (int) chromo[0] + ";\n");
					out.append("private static final long HIT_EVALUATE_INTERVAL = " + (long) chromo[1] + ";\n");
					out.append("private static final long HIT_WALL_EVALUATE_INTERVAL = " + (long) chromo[2] + ";\n");
					out.append("int numberOfHits = 0;\n");
					out.append("long firstHitTime = 0;\n");
					out.append("private byte directionMovementSign = 1;\n");
					out.append("double enemyBearing = 0;\n");
					out.append("int hitWallCount = 0;\n");
					out.append("long firstHitWallTime = 0;\n");
					out.append("Random rand = new Random();\n");
					out.append("MoveStrategy moveStrategy = MoveStrategy.ZIG_ZAG;\n");
					out.append("List<MoveStrategy> moveStrategyList = Arrays.asList(MoveStrategy.ZIG_ZAG, MoveStrategy.CIRCLE);\n");
					out.append("int strategyIndex = 0;\n");
					out.append("double enemyLastAbsBearing = 0.0;\n");

					out.append("public void run() {\n");
					out.append("setup();\n");

					out.append("while(true) {\n");

						out.append("if (getTime() - firstHitTime > HIT_EVALUATE_INTERVAL) {\n");
							out.append("System.out.println(\"Limite de tempo ultrapassado - zerando hits\");\n");

							out.append("numberOfHits = 0;\n");
							out.append("}\n");
						out.append("doMoveStrategy();\n");

						out.append("turnRadarRightRadians(Double.POSITIVE_INFINITY);\n");
						out.append("}\n");
					out.append("}\n");

					out.append("private void setup() {\n");
						out.append("setColors(Color.red, Color.blue, Color.white);\n");
					out.append("}\n");

					out.append("private WallNear isNearWall() {\n");
					out.append("if (getX() < 80.0) {\n");
							out.append("return WallNear.LEFT_WALL;\n");
					out.append("}\n");

						out.append("if (getX() > 740.0) {\n");
							out.append("return WallNear.RIGHT_WALL;\n");
							out.append("}\n");

						out.append("if (getY() < 80.0) {\n");
							out.append("return WallNear.BOTTOM_WALL;\n");
							out.append("}\n");

						out.append("if (getY() > 540.0) {\n");
							out.append("return WallNear.TOP_WALL;\n");
							out.append("}\n");

						out.append("return WallNear.NONE;\n");
						out.append("}\n");

					    /**
					     * onScannedRobot: What to do when you see another robot
					     */
					out.append("public void onScannedRobot(ScannedRobotEvent e) {\n");
						out.append("enemyBearing = e.getBearing();\n");
						out.append("doMoveStrategy();\n");
						out.append("doShootStrategy(e);\n");
						out.append("}\n");

					    /**
					     * onHitByBullet: What to do when you're hit by a bullet
					     */
					out.append("public void onHitByBullet(HitByBulletEvent e) {\n");
						out.append("numberOfHits++;\n");
						out.append("if(numberOfHits == 1) {\n");
							out.append("System.out.println(\"Primeiro HIT\");\n");

							out.append("firstHitTime = getTime();\n");
							out.append("}\n");

						out.append("if(numberOfHits >= HIT_NUMBERS_TO_SWITCH_MOVEMENT && (getTime() - firstHitTime <= HIT_EVALUATE_INTERVAL)){\n");
							out.append("System.out.println(\"Mudando estratégia\");\n");

							out.append("strategyIndex++;\n");
							out.append("int expectedIndex = strategyIndex % moveStrategyList.size();\n");
							out.append("moveStrategy = moveStrategyList.get(expectedIndex);\n");
							out.append("numberOfHits = 0;");
							out.append("}");

						out.append("}\n");

					    /**
					     * onHitWall: What to do when you hit a wall
					     */
					out.append("public void onHitWall(HitWallEvent e) {\n");
						out.append("System.out.println(\"HIT Wall\"\n);");

						out.append("hitWallCount++;\n");
						out.append("if(numberOfHits == 1) {\n");
							out.append("System.out.println(\"Primeiro HIT Wall\");\n");

							out.append("firstHitWallTime = getTime();\n");
							out.append("}");
						out.append("if (hitWallCount >= HIT_NUMBERS_TO_SWITCH_MOVEMENT && (getTime() - firstHitWallTime <= HIT_WALL_EVALUATE_INTERVAL)) {\n");
							out.append("hitWallCount = 0;\n");
							out.append("moveStrategy = MoveStrategy.CIRCLE;\n");
							out.append("}\n");
						out.append("}\n");

					out.append("enum WallNear {\n");
						out.append("NONE, LEFT_WALL, RIGHT_WALL, TOP_WALL, BOTTOM_WALL,\n");
						out.append("LEFT_BOTTOM_WALL, LEFT_TOP_WALL, RIGHT_TOP_WALL, RIGHT_BOTTOM_WALL\n");
						out.append("}\n");
					out.append("enum MoveStrategy {\n");
						out.append("ZIG_ZAG, CIRCLE\n");
						out.append("}\n");

					out.append("public void doMoveStrategy() {\n");
						out.append("switch (moveStrategy) {\n");
						out.append("case ZIG_ZAG:\n");
							out.append("System.out.println(\"Começando ZIG ZAG\");\n");

							out.append("doMoveInZigZag();\n");
							out.append("break;\n");
							out.append("case CIRCLE:\n");         
								out.append("System.out.println(\"Começando CIRCLE\");\n");              

								out.append("doMoveInCircle();\n");              
								out.append("break;\n");             
								out.append("}\n");      
						out.append("}\n");   

					out.append("public void doMoveInCircle() {\n");   
						out.append("setMaxVelocity(" + chromo[3] + ");\n");
						out.append("if (isNearWall() != WallNear.NONE || getVelocity() == 0) {\n");      
							out.append("directionMovementSign *= -1;directionMovementSign *= -1;\n");          
							out.append("}\n");      
						out.append("setTurnRight(setNormalizeAngleForBearing(enemyBearing + 90));\n");      
						out.append("setAhead(1000 * directionMovementSign);\n");       

						out.append("execute();\n");       
						out.append("}\n");   


					out.append("public void doMoveInZigZag() {\n"); 
						out.append("setMaxVelocity(" + chromo[4] + ");\n");

						out.append("setTurnRight(setNormalizeAngleForBearing(enemyBearing + 90));\n");       
						out.append("execute();\n");        

						out.append("System.out.println(getTime() % 20);\n");       

						out.append("if (getTime() % 20 == 0) {\n");        
							out.append("directionMovementSign *= -1;\n");           
							out.append("setAhead(150 * directionMovementSign);\n");          
					            out.append("execute();\n");          
					            out.append("System.out.println(\"moving\");\n");           

					            out.append("}\n");       
						out.append("}\n");

					out.append("double setNormalizeAngleForBearing(double angle) {\n");  

						out.append("while (angle >  180) {\n");      
							out.append("angle -= 360;\n");           
							out.append("}\n");       
						out.append("while (angle < -180) {\n");       
					        	out.append("angle += 360;\n");           
					        	out.append("}\n");      
						out.append("return angle;\n");      
						out.append("}\n");   

					out.append("void doShootStrategy(ScannedRobotEvent e) {\n");  
						out.append("System.out.println(\"Velocity: \" + e.getVelocity());\n");      
						out.append("System.out.println(\"Heading: \" + e.getHeading());\n");      

						out.append("double absoluteBearing = getHeadingRadians() + e.getBearingRadians();\n");     
						out.append("if (e.getVelocity() == 0.0) {\n");     
							out.append("setTurnGunRightRadians(\n");         
									out.append("robocode.util.Utils.normalRelativeAngle(absoluteBearing -\n");                 
											out.append("getGunHeadingRadians()));\n");                        
							out.append("} else {\n");     
								out.append("double futureBearing = Math.abs(absoluteBearing - enemyLastAbsBearing);\n");         
								out.append("setTurnGunRightRadians(\n");         
										out.append("robocode.util.Utils.normalRelativeAngle((absoluteBearing + futureBearing) -\n");                 
										out.append("getGunHeadingRadians()));\n");           
								out.append("}\n");     

						out.append("fire(calculateBulletPower(e.getDistance()));\n");   
						out.append("enemyLastAbsBearing = absoluteBearing;\n"); 
						out.append("}\n");

					out.append("double calculateBulletPower(double distance) {\n") ;
						out.append("return Math.max(" + chromo[5]+ " / (distance / " + chromo[6] + "), 0.5);\n");
						out.append("}\n");
					out.append("}\n");
			  
			out.close(); // close output stream
			
		} catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
}