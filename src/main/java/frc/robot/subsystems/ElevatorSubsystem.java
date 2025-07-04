package frc.robot.subsystems;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.ClosedLoopSlot;
import com.revrobotics.spark.config.ClosedLoopConfig.FeedbackSensor;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkMax;

public class ElevatorSubsystem extends SubsystemBase{

    private final SparkMax m_ElevatorLeftSpark; 
    private final SparkMax m_ElevatorRightSpark;
    private SparkClosedLoopController elevatorClosedLoopController;
    private RelativeEncoder encoder;
    private double currentspeed;
    private double currentposition;
    private double previousposition;
    public double elevatorspeedlimiter;
    private int closedLoopTarget;
    private boolean manualcontrol;
   

    public static final class ElevatorConstants {
        // SPARK MAX CAN IDs
        public static final int kElevatorLeftCanId = 9;
        public static final int kElevatorRightCanId = 10;

        // Speed
        public static final double kElevatorSpeed = 1.0;

        // Highest and lowest levels
        public static final double kLowestLevel = 0.0;
        public static final double kHighestLevel = 200.0;

        // Ascending and Descending levels to start slowing down
        public static final double kSlowdownLevelAscending = 190;
        public static final double kSlowdownLevelDescending = 70;

        // SparkMax Configurations
        public static final SparkMaxConfig leadConfig = new SparkMaxConfig();
        public static final SparkMaxConfig followConfig = new SparkMaxConfig();

        static {              
            leadConfig.smartCurrentLimit(50);
            leadConfig.idleMode(IdleMode.kBrake);  
            leadConfig.openLoopRampRate(2.0);   
            leadConfig.closedLoopRampRate(0.0);   
            leadConfig.closedLoop.feedbackSensor(FeedbackSensor.kPrimaryEncoder);
            leadConfig.closedLoop.pid(0.05, 0, 0);
            leadConfig.closedLoop.outputRange(-1,1);
            followConfig.apply(leadConfig);
            followConfig.inverted(true);
        }
    } 

    public ElevatorSubsystem(){

        // Left Elevator Motor 
        m_ElevatorLeftSpark = new SparkMax(ElevatorConstants.kElevatorLeftCanId, MotorType.kBrushless);
        m_ElevatorLeftSpark.configure(ElevatorConstants.leadConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

        // Right Elevator Motor  
        ElevatorConstants.followConfig.follow(m_ElevatorLeftSpark, true);       
        m_ElevatorRightSpark = new SparkMax(ElevatorConstants.kElevatorRightCanId, MotorType.kBrushless);   
        m_ElevatorRightSpark.configure(ElevatorConstants.followConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    
        // Elevator Encoder
        encoder = m_ElevatorLeftSpark.getEncoder();
        elevatorClosedLoopController = m_ElevatorLeftSpark.getClosedLoopController();
        manualcontrol = true;
    }

    public void init() {
        // Configure right Elevator Motor to follow left just in case this was missed at startup        
        ElevatorConstants.followConfig.follow(m_ElevatorLeftSpark, true);
        m_ElevatorRightSpark.configure(ElevatorConstants.followConfig, ResetMode.kNoResetSafeParameters, PersistMode.kNoPersistParameters);}


    private double scaledSpeedToTop() {        
        return ElevatorConstants.kElevatorSpeed * Math.min(ElevatorConstants.kSlowdownLevelAscending,(ElevatorConstants.kHighestLevel - currentposition))/ElevatorConstants.kSlowdownLevelAscending; }
    
    private double scaledSpeedToBottom() {        
        return -ElevatorConstants.kElevatorSpeed * Math.min(ElevatorConstants.kSlowdownLevelDescending, Math.max(currentposition,1))/ElevatorConstants.kSlowdownLevelDescending; }

    public void robotPeriodic() {
        currentposition = encoder.getPosition();        
        // Speed limiter used to limit swerve drive speed based on elevator height to prevent tipping with a higher center of gravity
        elevatorspeedlimiter = (ElevatorConstants.kHighestLevel + 70 - currentposition) / (ElevatorConstants.kHighestLevel + 70); }

    public void teleopPeriodic() {
        if (!manualcontrol) {
            elevatorClosedLoopController.setReference(closedLoopTarget, ControlType.kPosition, ClosedLoopSlot.kSlot0);
        }
    }

    public void raise() {
        currentposition = encoder.getPosition();
        manualcontrol = true;
        if (currentposition < ElevatorConstants.kHighestLevel) {
            currentspeed = scaledSpeedToTop();
            m_ElevatorLeftSpark.set(currentspeed);}
        else {
            m_ElevatorLeftSpark.stopMotor();}}

    public void lower() {    
        manualcontrol = true;
        currentposition = encoder.getPosition();    
        if (currentposition > ElevatorConstants.kLowestLevel) {
            currentspeed = scaledSpeedToBottom();
            m_ElevatorLeftSpark.set(currentspeed);}
        else {
            m_ElevatorLeftSpark.stopMotor();}}

    public void stop() {
        m_ElevatorLeftSpark.stopMotor(); }


    public void GoToClosedLoopPosition(int targetPosition) {
        manualcontrol = false;
        closedLoopTarget = targetPosition;        
    } 

    public void testPeriodic() {
        currentposition = encoder.getPosition();
        if (previousposition != currentposition)
            System.out.println("Elevator position: " + currentposition);
        previousposition = currentposition;
    }

}
