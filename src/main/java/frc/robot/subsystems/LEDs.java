// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.ctre.phoenix.led.Animation;
import com.ctre.phoenix.led.CANdle;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.constLEDs;
import frc.robot.RobotMap.mapLEDs;

public class LEDs extends SubsystemBase {

  CANdle CANdle;

  /** Creates a new LEDs. */
  public LEDs() {
    CANdle = new CANdle(mapLEDs.CANDLE_CAN);
    configure();
  }

  public void configure() {
    CANdle.configFactoryDefault();
    CANdle.configBrightnessScalar(constLEDs.LED_BRIGHTNESS);
  }

  public void setLEDs(int[] rgb) {
    CANdle.setLEDs(rgb[0], rgb[1], rgb[2]);
  }

  /**
   * <b>Types of Animations available:</b>
   * ColorFlowAnimation, FireAnimation, LarsonAnimation, RainbowAnimation,
   * RgbFadeAnimation, SingleFadeAnimation, StrobeAnimation, TwinkleAnimation,
   * TwinkleOffAnimation
   * 
   * <b>Important Note:</b>
   * You cannot apply multiple animations to the same animation slot at the same
   * time
   * 
   * @param animation     Type of Animation desired
   * @param animationSlot The animation slot to apply the animation to (starts
   *                      from 0).
   */
  public void setLEDAnimation(Animation animation, int animationSlot) {
    CANdle.animate(animation, animationSlot);
  }

  /**
   * Sets a certain LED to a desired color
   * 
   * @param rgb           How much red, green, and blue the color has
   * @param LEDStartIndex The index number of the specific LED to control,
   *                      starting at 0
   * @param LEDLength     How many LEDs to light up following that number
   */
  public void setLEDMatrix(int[] rgb, int LEDStartIndex, int LEDLength) {
    CANdle.setLEDs(rgb[0], rgb[1], rgb[2], 0, LEDStartIndex, LEDLength);
  }

  public void clearAnimation() {
    CANdle.clearAnimation(0);
    CANdle.clearAnimation(1);
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
  }
}
