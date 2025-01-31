// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import com.frcteam3255.joystick.SN_Extreme3DStick;
import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.units.Units;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.PowerDistribution.ModuleType;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Constants.constControllers;
import frc.robot.Constants.constElevator;
import frc.robot.Constants.constField;
import frc.robot.Constants.constLEDs;
import frc.robot.Constants.constShooter;
import frc.robot.RobotMap.mapControllers;
import frc.robot.RobotPreferences.prefVision;
import frc.robot.commands.AddVisionMeasurement;
import frc.robot.commands.Drive;
import frc.robot.commands.Autos.Centerline;
import frc.robot.commands.Autos.PreloadOnly;
import frc.robot.commands.Autos.PreloadTaxi;
import frc.robot.commands.Autos.WingOnly;
import frc.robot.commands.Zeroing.ManualZeroElevator;
import frc.robot.commands.Zeroing.ManualZeroShooterPivot;
import frc.robot.commands.Zeroing.ZeroElevator;
import frc.robot.commands.Zeroing.ZeroShooterPivot;
import frc.robot.commands.States.IntakeSource;
import frc.robot.subsystems.Climber;
import frc.robot.subsystems.Drivetrain;
import frc.robot.subsystems.Elevator;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.LEDs;
import frc.robot.subsystems.Shooter;
import frc.robot.subsystems.StateMachine;
import frc.robot.subsystems.Transfer;
import frc.robot.subsystems.StateMachine.RobotState;
import frc.robot.subsystems.Limelight;

public class RobotContainer {

  private final SN_Extreme3DStick conDriver = new SN_Extreme3DStick(mapControllers.DRIVER_USB);

  private final static Climber subClimber = new Climber();
  private final static Drivetrain subDrivetrain = new Drivetrain();
  private final static Elevator subElevator = new Elevator();
  private final static Intake subIntake = new Intake();
  private final static LEDs subLEDs = new LEDs();
  private final static Shooter subShooter = new Shooter();
  private final static Transfer subTransfer = new Transfer();
  private final static Limelight subLimelight = new Limelight();
  public final static StateMachine subStateMachine = new StateMachine(subClimber, subDrivetrain,
      subElevator, subIntake, subLEDs, subTransfer, subShooter);

  private final Trigger falseTrigger = new Trigger(() -> false);
  private final Trigger gamePieceStoredTrigger = new Trigger(() -> subTransfer.getGamePieceStored());
  private final Trigger gamePieceCollectedTrigger = falseTrigger;

  private final BooleanSupplier readyToShootOperator = (() -> (subDrivetrain.isDrivetrainFacingSpeaker()
      || subDrivetrain.isDrivetrainFacingShuffle())
      && subShooter.readyToShoot() && subStateMachine.isCurrentStateTargetState()
      && subTransfer.getGamePieceStored());

  private final BooleanSupplier readyToShootDriver = (() -> subShooter.readyToShoot()
      && subStateMachine.isCurrentStateTargetState() && subTransfer.getGamePieceStored());

  private final BooleanSupplier readyToShootAuto = (() -> (subDrivetrain.isDrivetrainFacingSpeaker()
      || subDrivetrain.isDrivetrainFacingShuffle())
      && subShooter.readyToShoot());

  private final BooleanSupplier readyToShootSpeakerLEDs = (() -> subDrivetrain.isDrivetrainFacingSpeaker()
      && subShooter.readyToShoot() && subStateMachine.getRobotState() == RobotState.PREP_VISION
      && subTransfer.getGamePieceStored());

  private final BooleanSupplier readyToShootShuffleLEDs = (() -> subDrivetrain.isDrivetrainFacingShuffle()
      && subShooter.readyToShoot() && subStateMachine.getRobotState() == RobotState.PREP_SHUFFLE
      && subTransfer.getGamePieceStored());

  private final IntakeSource comIntakeSource = new IntakeSource(subStateMachine, subShooter, subTransfer);

  SendableChooser<Command> autoChooser = new SendableChooser<>();

  private static PowerDistribution PDH = new PowerDistribution(1, ModuleType.kRev);

  public RobotContainer() {
    
    subDrivetrain.setDefaultCommand(
        new Drive(subDrivetrain, subStateMachine, conDriver.getYAxis(), conDriver.getXAxis(), conDriver.getTwistAxis(),
             falseTrigger, falseTrigger, falseTrigger, falseTrigger, falseTrigger, falseTrigger, falseTrigger, falseTrigger));

    // - Manual Triggers -
    gamePieceStoredTrigger
        .onTrue(Commands
            .deferredProxy(
                () -> subStateMachine.tryState(RobotState.STORE_FEEDER))
            .andThen(Commands.deferredProxy(
                () -> subStateMachine.tryTargetState(subStateMachine, subIntake, subLEDs, subShooter, subTransfer,
                    subElevator, subDrivetrain))))
        .onTrue(Commands.runOnce(() -> subTransfer.setGamePieceCollected(true)));

    gamePieceCollectedTrigger
        .onTrue(Commands
            .runOnce(() -> conDriver.setRumble(RumbleType.kLeftRumble, constControllers.DRIVER_GP_COLLECTED_RUMBLE)))
        .onTrue(Commands.runOnce(() -> subLEDs.setLEDs(constLEDs.GAME_PIECE_COLLECTED_COLOR)));

    new Trigger(readyToShootOperator).onTrue(
        Commands.runOnce(() -> conDriver.setRumble(RumbleType.kBothRumble,
            constControllers.OPERATOR_RUMBLE)))
        .onFalse(
            Commands.runOnce(() -> conDriver.setRumble(RumbleType.kBothRumble, 0)));

    new Trigger(readyToShootDriver).onTrue(
        Commands.runOnce(() -> conDriver.setRumble(RumbleType.kBothRumble,
            constControllers.DRIVER_RUMBLE)))
        .onFalse(
            Commands.runOnce(() -> conDriver.setRumble(RumbleType.kBothRumble,
                0)));

    new Trigger(readyToShootSpeakerLEDs)
        .onTrue(Commands.runOnce(() -> subLEDs.setLEDAnimation(constLEDs.READY_TO_SHOOT_COLOR, 0)))
        .onFalse(Commands.runOnce(() -> subLEDs.clearAnimation()));

    new Trigger(readyToShootShuffleLEDs)
        .onTrue(Commands.runOnce(() -> subLEDs.setLEDAnimation(constLEDs.READY_TO_SHOOT_COLOR, 0)))
        .onFalse(Commands.runOnce(() -> subLEDs.clearAnimation()));

    subDrivetrain.resetModulesToAbsolute();

    NamedCommands.registerCommand("Intaking", Commands.deferredProxy(
        () -> subStateMachine.tryState(RobotState.INTAKING))
        .until(gamePieceStoredTrigger));

    SmartDashboard.putNumber("Preload Only Delay", 0);

    configureDriverBindings(conDriver);
    configureAutoSelector();
  }

  private void configureDriverBindings(SN_Extreme3DStick controller) {
    // Reset Pose
    controller.btn_1.onTrue(
        Commands.runOnce(() -> subDrivetrain.resetPoseToPose(constField.getFieldPositions().get()[6].toPose2d())));

    // Intake from source
    controller.btn_2.whileTrue(Commands.deferredProxy(() -> subStateMachine.tryState(RobotState.INTAKE_SOURCE)))
        .onFalse(Commands.deferredProxy(() -> subStateMachine.tryState(RobotState.NONE))
            .unless(() -> comIntakeSource.getIntakeSourceGamePiece()));
  }

  private void configureAutoSelector() {
    DoubleSupplier preloadDelay = () -> SmartDashboard.getNumber("Preload Only Auto", 0);

    // -- Preload Sub --
    autoChooser.addOption("Preload Only Amp-Side", new PreloadOnly(subStateMachine, subClimber, subDrivetrain,
        subElevator, subIntake, subLEDs, subShooter, subTransfer, 0, preloadDelay));
    autoChooser.setDefaultOption("Preload Only Center",
        new PreloadOnly(subStateMachine, subClimber, subDrivetrain, subElevator,
            subIntake, subLEDs, subShooter, subTransfer,
            1, preloadDelay));
    autoChooser.addOption("Preload Only Source-Side", new PreloadOnly(subStateMachine, subClimber, subDrivetrain,
        subElevator, subIntake, subLEDs, subShooter, subTransfer, 2, preloadDelay));

    autoChooser.addOption("Preload Taxi",
        new PreloadTaxi(subStateMachine, subClimber, subDrivetrain, subElevator,
            subIntake, subLEDs, subShooter, subTransfer));
    autoChooser.addOption("Wing Only Down", new WingOnly(subStateMachine,
        subClimber, subDrivetrain, subElevator,
        subIntake, subLEDs, subTransfer, subShooter, readyToShootOperator, true));
    autoChooser.addOption("Wing Only Up", new WingOnly(subStateMachine,
        subClimber, subDrivetrain, subElevator,
        subIntake, subLEDs, subTransfer, subShooter, readyToShootOperator, false));

    autoChooser.addOption("Centerline :3", new Centerline(subStateMachine,
        subClimber, subDrivetrain, subElevator,
        subIntake, subLEDs, subTransfer, subShooter, readyToShootAuto, false));
    SmartDashboard.putData(autoChooser);
  }

  public Command getAutonomousCommand() {
    return autoChooser.getSelected();
  }

  /**
   * Returns the command to zero all subsystems. This will make all subsystems
   * move
   * themselves downwards until they see a current spike and cancel any incoming
   * commands that
   * require those motors. If the zeroing does not end within a certain time
   * frame (set in constants), it will interrupt itself.
   * 
   * @return Parallel commands to zero the Climber, Elevator, and Shooter Pivot
   */
  public static Command zeroSubsystems() {
    Command returnedCommand = new ParallelCommandGroup(
        new ZeroElevator(subElevator).withTimeout(constElevator.ZEROING_TIMEOUT.in(Units.Seconds)),
        new ZeroShooterPivot(subShooter).withTimeout(constShooter.ZEROING_TIMEOUT.in(Units.Seconds)))
        .withInterruptBehavior(Command.InterruptionBehavior.kCancelIncoming);
    returnedCommand.addRequirements(subStateMachine);
    return returnedCommand;
  }

  public static Command checkForManualZeroing() {
    return new ManualZeroShooterPivot(subShooter).alongWith(new ManualZeroElevator(subElevator)).ignoringDisable(true);
  }

  public static Command AddVisionMeasurement() {
    return new AddVisionMeasurement(subDrivetrain, subLimelight)
        .withInterruptBehavior(Command.InterruptionBehavior.kCancelIncoming).ignoringDisable(true);
  }

  public void setMegaTag2(boolean setMegaTag2) {

    if (setMegaTag2) {
      subDrivetrain.swervePoseEstimator.setVisionMeasurementStdDevs(VecBuilder.fill(
          prefVision.megaTag2StdDevsPosition.getValue(),
          prefVision.megaTag2StdDevsPosition.getValue(),
          prefVision.megaTag2StdDevsHeading.getValue()));
    } else {
      // Use MegaTag 1
      subDrivetrain.swervePoseEstimator.setVisionMeasurementStdDevs(VecBuilder.fill(
          prefVision.megaTag1StdDevsPosition.getValue(),
          prefVision.megaTag1StdDevsPosition.getValue(),
          prefVision.megaTag1StdDevsHeading.getValue()));
    }
    subLimelight.setMegaTag2(setMegaTag2);
  }

  // -- PDH --
  /**
   * Updates the values supplied to the PDH to SmartDashboard. Should be called
   * periodically.
   */
  public static void logPDHValues() {
    SmartDashboard.putNumber("PDH/Input Voltage", PDH.getVoltage());
    SmartDashboard.putBoolean("PDH/Is Switchable Channel Powered", PDH.getSwitchableChannel());
    SmartDashboard.putNumber("PDH/Total Current", PDH.getTotalCurrent());
    SmartDashboard.putNumber("PDH/Total Power", PDH.getTotalPower());
    SmartDashboard.putNumber("PDH/Total Energy", PDH.getTotalEnergy());

    if (Constants.ENABLE_PDH_LOGGING) {
      for (int i = 0; i < Constants.PDH_DEVICES.length; i++) {
        SmartDashboard.putNumber("PDH/" + Constants.PDH_DEVICES[i] + " Current", PDH.getCurrent(i));
      }
    }
  }

  // -- LEDS --
  public void setDisabledLEDs() {
    subLEDs.setLEDAnimation(constLEDs.DISABLED_COLOR_1, 0);
    subLEDs.setLEDAnimation(constLEDs.DISABLED_COLOR_2, 1);
  }

  public void setZeroedLEDs() {
    int[] elevatorRGB = new int[3];
    int[] shooterRGB = new int[3];

    if (Elevator.hasZeroed) {
      elevatorRGB = constLEDs.ELEVATOR_ZEROED;
    } else if (Elevator.attemptingZeroing) {
      elevatorRGB = constLEDs.ELEVATOR_ATTEMPTING_ZERO;
    } else {
      elevatorRGB = constLEDs.ELEVATOR_NOT_ZEROED;
    }

    if (Shooter.hasZeroed) {
      shooterRGB = constLEDs.SHOOTER_ZEROED;
    } else if (Shooter.attemptingZeroing) {
      shooterRGB = constLEDs.SHOOTER_ATTEMPTING_ZERO;
    } else {
      shooterRGB = constLEDs.SHOOTER_NOT_ZEROED;
    }

    subLEDs.setLEDMatrix(elevatorRGB, 0, 2);
    subLEDs.setLEDMatrix(elevatorRGB, 6, 2);
    subLEDs.setLEDMatrix(shooterRGB, 2, 4);
  }

  public void clearLEDs() {
    subLEDs.clearAnimation();
    subLEDs.setLEDs(constLEDs.CLEAR_LEDS);
  }

}
