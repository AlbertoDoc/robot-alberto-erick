package albertoErick;

import org.encog.engine.network.activation.ActivationReLU;
import robocode.*;

import java.io.*;
import java.util.*;
import java.awt.*;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.IBayesInferer;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;

import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.util.simple.EncogUtility;

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

    String datasetFileName = "datasetAlbertoErick.txt";
    private RNAState state = RNAState.EXECUTING;
    private Bullet shootInProgress = null;
    private StringBuilder newData = null;
    private double enemyX = 0.0;
    private double enemyY = 0.0;
    String separator = ", ";

    // Neural network weights and bias layers
    private double[][] input = new double[10][1];
    private double[][] layer0Biases = new double[128][1];
    private double[][] layer0Weights = new double[10][128];
    private double[][] layer1Biases = new double[64][1];
    private double[][] layer1Weights = new double[128][64];
    private double[][] layer2Biases = new double[32][1];
    private double[][] layer2Weights = new double[64][32];
    private double[][] layer3Biases = new double[2][1];
    private double[][] layer3Weights = new double[32][2];

    private BasicNetwork neuralNetwork;

    public void run() {
        // Initialize fuzzy
        fis = FIS.load(fclFilePath, true);
        firePower = fis.getFunctionBlock("firePower");
        prediction = fis.getFunctionBlock("prediction");

        setup();
        // Initialize the Bayesian network
        initializeBayesianNetwork();
        layer0Biases = readMatrixFromCSV(128, 1, "./robots/albertoErick/layer_0_biases.csv");
        layer0Weights = readMatrixFromCSV(10, 128, "./robots/albertoErick/layer_0_weights.csv");
        layer1Biases = readMatrixFromCSV(64, 1, "./robots/albertoErick/layer_1_biases.csv");
        layer1Weights = readMatrixFromCSV(128, 64, "./robots/albertoErick/layer_1_weights.csv");
        layer2Biases = readMatrixFromCSV(32, 1, "./robots/albertoErick/layer_2_biases.csv");
        layer2Weights = readMatrixFromCSV(64, 32, "./robots/albertoErick/layer_2_weights.csv");
        layer3Biases = readMatrixFromCSV(2, 1, "./robots/albertoErick/layer_3_biases.csv");
        layer3Weights = readMatrixFromCSV(32, 2, "./robots/albertoErick/layer_3_weights.csv");
        System.out.println(layer3Weights[0][0]);
        System.out.println(layer3Weights[31][1]);

        neuralNetwork = new BasicNetwork();
        neuralNetwork.addLayer(new BasicLayer(null, false, 10)); // Camada de entrada
        neuralNetwork.addLayer(new BasicLayer(new ActivationReLU(), true, 128)); // Camada escondida 1
        neuralNetwork.addLayer(new BasicLayer(new ActivationReLU(), true, 64)); // Camada escondida 2
        neuralNetwork.addLayer(new BasicLayer(new ActivationReLU(), true, 32)); // Camada escondida 3
        neuralNetwork.addLayer(new BasicLayer(new ActivationReLU(), true, 2)); // Camada de saída
        neuralNetwork.getStructure().finalizeStructure();
        neuralNetwork.reset();

        setWeightsLayer0();

        while(true) {
            if (getTime() - firstHitTime > HIT_EVALUATE_INTERVAL) {
              //  System.out.println("Limite de tempo ultrapassado - zerando hits");

                numberOfHits = 0;
            }
           // System.out.println("move strategy");

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

        // Calcular o ângulo absoluto em relação ao robô escaneado
        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
        enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);

        if ((System.currentTimeMillis() - hitTooMuchTime) > HIT_TOO_MUCH_EVALUATE_INTERVAL) {
            // System.out.println("Resetando hit too much");

            hitTooMuch = false;
        }
    }

    /**
     * onHitByBullet: What to do when you're hit by a bullet
     */
    public void onHitByBullet(HitByBulletEvent e) {
        numberOfHits++;
        if(numberOfHits == 1) {
          //  System.out.println("Primeiro HIT");

            firstHitTime = System.currentTimeMillis();
        }

        if(numberOfHits >= HIT_NUMBERS_TO_SWITCH_MOVEMENT && (System.currentTimeMillis() - firstHitTime <= HIT_EVALUATE_INTERVAL)){
         //   System.out.println("Mudando estratégia");
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
    //    System.out.println("HIT Wall");

        hitWallCount++;
        if(numberOfHits == 1) {
          //  System.out.println("Primeiro HIT Wall");

            firstHitWallTime = getTime();
        }

        if (hitWallCount >= HIT_NUMBERS_TO_SWITCH_MOVEMENT && (getTime() - firstHitWallTime <= HIT_WALL_EVALUATE_INTERVAL)) {
         //   System.out.println("Too much wall hit, changing to circle");
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
     //   System.out.println("proximo a parede: "+ isNearWall().toString());
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
       // System.out.println(vLowerLimit + " LOWER LIMIT V");

        switch (moveStrategy) {
            case ZIG_ZAG:
              //  System.out.println("Começando ZIG ZAG");

                doMoveInZigZag(vLowerLimit);
                break;
            case CIRCLE:
            //    System.out.println("Começando CIRCLE");

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

      //  System.out.println(getTime() % 20);

        if (getTime() % 20 == 0) {
            directionMovementSign *= -1;
            setAhead(150 * directionMovementSign);
            execute();
         //   System.out.println("moving");

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
      //  System.out.println("Velocity: " + e.getVelocity());
      //  System.out.println("Heading: " + e.getHeading());

        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        // double futureBearing = Math.abs(absoluteBearing - enemyLastAbsBearing);
       // if (e.getVelocity() == 0.0) {
            setTurnGunRightRadians(
                    robocode.util.Utils.normalRelativeAngle(absoluteBearing -
                            getGunHeadingRadians()));
       // } else {
         //   setTurnGunRightRadians(
          //          robocode.util.Utils.normalRelativeAngle((absoluteBearing + futureBearing) -
           //                 getGunHeadingRadians()));
      //  }

      //  System.out.println("aimCorrection: " + futureBearing);

        if (shootInProgress == null) {
            switch (state) {
                case COLLECTING: collectEvent(e, absoluteBearing);
                    break;
                case EXECUTING:
                    double[][] output = predict(e, absoluteBearing);
                    shootInProgress = fireBullet(calculateBulletPower(getEnergy(), e.getDistance(), 0, e.getVelocity()));
                    break;
            }
        }
        enemyLastAbsBearing = absoluteBearing;
    }

    double calculateBulletPower(double ourEnergy, double distance, double prediction, double enemySpeed) {
        firePower.setVariable("ourEnergy", ourEnergy);
        firePower.setVariable("distance", distance);
        firePower.setVariable("prediction", prediction);
        firePower.setVariable("enemySpeed", Math.abs(enemySpeed));

     //   System.out.println("ourEnergy" + ourEnergy);
     //   System.out.println("distance" + distance);
     //   System.out.println("prediction" + prediction);
     //   System.out.println("enemySpeed" + Math.abs(enemySpeed));

        firePower.evaluate();

        double result = firePower.getVariable("firePower").getValue();
       // System.out.println("poder tiro: " + result);

        return result;
    }

    enum RNAState {
        COLLECTING, EXECUTING
    }

    FileWriter openDataset() {
        try {
            // Cria o arquivo caso não exista
            File dataset = new File(datasetFileName);

            if (dataset.createNewFile()) {
                FileWriter writer = new FileWriter(datasetFileName, true);
                writer.write("Posição x inicial inimigo, " +
                        "Posição y inicial inimigo,  " +
                        "Posição x inicial nossa,  " +
                        "Posição y inicial nossa, " +
                        "Velocidade inicial inimigo, " +
                        "Velocidade inicial nossa, " +
                        "Estratégia de movimentação, " +
                        "Angulo utilizado para disparar, " +
                        "Posição x final do inimigo, " +
                        "Posição Y final do inimigo, " +
                        "Posição x final nossa, " +
                        "Posição Y final nossa\n"
                );
                writer.close();
            }

            // Instancia objeto que escreve no arquivo
            return new FileWriter(datasetFileName, true);
        } catch (IOException e) {
            System.out.println("An error has occurred.");
            e.printStackTrace();
            return null;
        }
    }

    void collectEvent(ScannedRobotEvent event, double absoluteBearing) {
        // Estrutura do dataset: CSV -> X (nosso), Y (nosso), Velocidade (inimigo), Estrat. Movimentação
        String separator = ", ";

        // Refinamento de dados em formato numerico
        int moveStrategyNumber = moveStrategy == MoveStrategy.CIRCLE ? 0 : 1;
        double angleToEnemy = event.getBearing();

        double angle = Math.toRadians(getHeading() + angleToEnemy % 360);

        double enemyX = (getX() + Math.sin(angle) * event.getDistance());
        double enemyY = (getY() + Math.cos(angle) * event.getDistance());

        // Escrever no arquivo
        if (newData == null && shootInProgress == null) {
            input[0][0] = enemyX;
            input[1][0] = enemyY;
            input[2][0] = getX();
            input[3][0] = getY();
            input[4][0] = event.getVelocity();
            input[5][0] = getVelocity();
            input[6][0] = moveStrategyNumber;
            input[7][0] = absoluteBearing;
            input[8][0] = getX();
            input[9][0] = getY();
            newData = new StringBuilder();
            newData.append(enemyX).append(separator)
                    .append(enemyY).append(separator)
                    .append(getX()).append(separator)
                    .append(getY()).append(separator)
                    .append(event.getVelocity()).append(separator)
                    .append(getVelocity()).append(separator)
                    .append(moveStrategyNumber).append(separator)
                    .append(absoluteBearing).append(separator);
        }
    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        // Esta condição verifica se o bullet do evento é igual ao que armazenamos no disparo
        if (shootInProgress.hashCode() == event.getBullet().hashCode()) {
            try {
                // Resetando valores para coletar outro dado
                shootInProgress = null;
                newData = null;
            } catch (NullPointerException e) {
                System.out.println("onBulletHit NullPointerException thrown");
            }
        }

        // É ativado quando o tiro acerta no oponente
        super.onBulletHit(event);
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
        if (shootInProgress.hashCode() == event.getBullet().hashCode()) {
            // Verificar se precisamos armazenar que erramos esse tiro, pois pode ser uma coincidencia ter acertado o disparo do inimigo
            try {
                // Resetando valores para coletar outro dado
                shootInProgress = null;
                newData = null;
            } catch (NullPointerException e) {
                System.out.println("onBulletHitBullet NullPointerException thrown");
            }
        }

        // É ativado quando o tiro acerta um tiro adversário
        super.onBulletHitBullet(event);
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        if (shootInProgress.hashCode() == event.getBullet().hashCode()) {
            try {
                // Resetando valores para coletar outro dado
                shootInProgress = null;
                newData = null;
            } catch (NullPointerException e) {
                System.out.println("onBulletMissed NullPointerException thrown");
            }
        }

        // É ativado quando o tiro acerta a parede
        super.onBulletMissed(event);
    }

    public double[][] readMatrixFromCSV(int rows, int columns, String filePath) {
        String linha = "";
        String separador = ","; // Separador de campos, geralmente uma vírgula
        double[][] matrix = new double[rows][columns];

        int i = 0;
        int j = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((linha = br.readLine()) != null) {
                // Use o método split para dividir a linha em campos
                String[] campos = linha.split(separador);

                // Exemplo: Imprime todos os campos da linha
                for (String campo : campos) {
                    matrix[i][j] = Double.parseDouble(campo);
                    j++;
                }
                i++;
                j = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return matrix;
    }

    private static double[][] multiplyMatrices(double[][] a, double[][] b) {
        double[][] result = new double[a.length][b[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < b[0].length; j++) {
                for (int k = 0; k < b.length; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }

    private static double[][] addMatrices(double[][] a, double[][] b) {
        double[][] result = new double[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                result[i][j] = a[i][j] + b[i][j];
            }
        }
        return result;
    }

    private static double[][] relu(double[][] matrix) {
        double[][] result = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                result[i][j] = Math.max(0, matrix[i][j]);
            }
        }
        return result;
    }

    private static double[][] transposeMatrix(double[][] matrix) {
        double[][] result = new double[matrix[0].length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                result[j][i] = matrix[i][j];
            }
        }
        return result;
    }

    private static void printMatrix(double[][] matrix) {
        for (double[] row : matrix) {
            for (double value : row) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }

    private double[][] predict(ScannedRobotEvent event, double absoluteBearing) {
        int moveStrategyNumber = moveStrategy == MoveStrategy.CIRCLE ? 0 : 1;
        double angleToEnemy = event.getBearing();

        double angle = Math.toRadians(getHeading() + angleToEnemy % 360);

        double enemyX = (getX() + Math.sin(angle) * event.getDistance());
        double enemyY = (getY() + Math.cos(angle) * event.getDistance());

        input[0][0] = enemyX;
        input[1][0] = enemyY;
        input[2][0] = getX();
        input[3][0] = getY();
        input[4][0] = event.getVelocity();
        input[5][0] = getVelocity();
        input[6][0] = moveStrategyNumber;
        input[7][0] = absoluteBearing;
        input[8][0] = getX();
        input[9][0] = getY();

        double[][] layer0_output = relu(addMatrices(multiplyMatrices(transposeMatrix(layer0Weights), input), layer0Biases));
        double[][] layer1_output = relu(addMatrices(multiplyMatrices(transposeMatrix(layer1Weights), layer0_output), layer1Biases));
        double[][] layer2_output = relu(addMatrices(multiplyMatrices(transposeMatrix(layer2Weights), layer1_output), layer2Biases));
        double[][] layer3_output = addMatrices(multiplyMatrices(transposeMatrix(layer3Weights), layer2_output), layer3Biases);

        System.out.println("Saída da rede neural:");
        printMatrix(layer3_output);
        return layer3_output;
    }

    private void setWeightsLayer0() {
    }
}
