package org.firstinspires.ftc.teamcode.drive;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvWebcam;

import java.util.ArrayList;

@Config
@TeleOp(group = "Main")
public class finalDriveBlueCameraBoi extends LinearOpMode {

    //Variables that allows the lift to hold in place
    int rightLiftHeight = 0;
    int leftLiftHeight = 0;
    int topHeight = 1100;
    double halfWay = 192.25;

    int range;
    public static int upperBound = 20;
    public static int lowerBound = 25;

    //Booleans that allow the an action to happen once and not cycle if pressed
    ArrayList<Boolean> booleanArray = new ArrayList<>();
    int booleanIncrementer = 0;
    boolean start1Pressed;
    boolean back1Pressed;
    boolean a2Pressed;
    boolean x2Pressed;
    boolean y2Pressed;
    boolean r3_2Pressed;
    boolean leftDpad2Pressed;

    //Creates the webcam
    OpenCvWebcam cam;

    //Elapsed timer used for the time on the outtake
    ElapsedTime outtakeTime = new ElapsedTime(0);

    //Finite state machine that allows the box to work
    ConfigurationStorage.boxState boxState = ConfigurationStorage.boxState.inside;
    ConfigurationStorage.horizontalSlideState horizontalSlideState = ConfigurationStorage.horizontalSlideState.inside;

    //Finite state machine that keeps track of the automatic outtake
    ConfigurationStorage.intakeMode intakeMode = ConfigurationStorage.intakeMode.manual;
    ConfigurationStorage.runOuttake runOuttake = ConfigurationStorage.runOuttake.openToRun;
    ConfigurationStorage.triggerHeld triggerHeld = ConfigurationStorage.triggerHeld.notBeingHeld;

    //Creates SampleMecanumDrive to be used for Roadrunner
    SampleMecanumDrive d;

    //Creates the FtcDashboard that is used for debugging
    private final FtcDashboard dashboard = FtcDashboard.getInstance();

    @Override
    public void runOpMode() throws InterruptedException {
        //Hardware maps the SampleMecanumDrive
        d = new SampleMecanumDrive(hardwareMap);

        //Allows telemetry to be used on the dashboard
        telemetry = new MultipleTelemetry(telemetry, dashboard.getTelemetry());

        //Does not allow encoders to directly affect the driving of the robot
        d.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        //Set the pose estimate the robot knows what orientation is for field centric driving
        d.setPoseEstimate(PoseStorage.telePowerBlue);
        d.leftLiftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        d.rightLiftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        d.leftLiftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        d.rightLiftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        d.intake.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        d.intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        //Hardware maps the webcam and create a way to view what the camera sees
        int cameraMonitorViewId = hardwareMap.appContext.getResources()
                .getIdentifier("cameraMonitorViewId", "id",
                        hardwareMap.appContext.getPackageName());

        cam = OpenCvCameraFactory.getInstance()
                .createWebcam(hardwareMap.get
                        (WebcamName.class, "Webcam 1"), cameraMonitorViewId);

        //Opens the camera and sets the openCV code to the webcam
        cam.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                cam.startStreaming(640, 480, OpenCvCameraRotation.UPRIGHT);
            }

            //Runs if the camera fails to open
            @Override
            public void onError(int errorCode) {
                cam.closeCameraDevice();
                telemetry.addData("errorCode:", errorCode);
                telemetry.update();
            }
        });

        //Allows the dashboard to see what the camera sees
        FtcDashboard.getInstance().startCameraStream(cam, 30);
        waitForStart();

        //After the robot starts, set positions for the start
        d.leftLinkage.setPosition(0);
        d.rightLinkage.setPosition(0);
        d.rightBox.setPosition(0);
        d.leftBox.setPosition(0);

        //The loop that allows the code to keep driving and do actions
        while (opModeIsActive() && !isStopRequested()) {
            colorSensor();
            driving();
            action();
        }
    }

    private void action() {

        //Uses the D-pad for the lift
        if (gamepad2.dpad_up && !gamepad2.dpad_down && (d.rightLiftMotor.getCurrentPosition() < topHeight && d.leftLiftMotor.getCurrentPosition() < topHeight)) {
            rightLiftHeight = d.rightLiftMotor.getCurrentPosition();
            leftLiftHeight = d.leftLiftMotor.getCurrentPosition();
            d.rightLiftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            d.leftLiftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            d.rightLiftMotor.setPower(0.8);
            d.leftLiftMotor.setPower(0.8);
        } else if (gamepad2.dpad_down && !gamepad2.dpad_up) {
            rightLiftHeight = d.rightLiftMotor.getCurrentPosition();
            leftLiftHeight = d.leftLiftMotor.getCurrentPosition();
            d.rightLiftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            d.leftLiftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            d.rightLiftMotor.setPower(-0.6);
            d.leftLiftMotor.setPower(-0.6);
        } else {
            d.rightLiftMotor.setTargetPosition(rightLiftHeight);
            d.leftLiftMotor.setTargetPosition(leftLiftHeight);

            d.leftLiftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            d.rightLiftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            d.leftLiftMotor.setPower(0.1);
            d.rightLiftMotor.setPower(0.1);
        }

        //Pressing A activates the horizontal slides
        if (a2Pressed && d.rightLiftMotor.getCurrentPosition() > 200 && d.leftLiftMotor.getCurrentPosition() > 200) {
            if (horizontalSlideState == ConfigurationStorage.horizontalSlideState.inside) {
                d.leftLinkage.setPosition(1);
                d.rightLinkage.setPosition(1);
                horizontalSlideState = ConfigurationStorage.horizontalSlideState.outside;
            } else if ((horizontalSlideState == ConfigurationStorage.horizontalSlideState.outside) || (horizontalSlideState == ConfigurationStorage.horizontalSlideState.halfway)) {
                d.leftLinkage.setPosition(0);
                d.rightLinkage.setPosition(0);
                horizontalSlideState = ConfigurationStorage.horizontalSlideState.inside;
            }
        } else if (a2Pressed && d.rightLiftMotor.getCurrentPosition() < 200 && d.leftLiftMotor.getCurrentPosition() < 200) {
            if (horizontalSlideState == ConfigurationStorage.horizontalSlideState.inside) {
                d.leftLinkage.setPosition(0.4);
                d.rightLinkage.setPosition(0.4);
                d.rightBox.setPosition(0.4);
                d.leftBox.setPosition(0.4);
                boxState = ConfigurationStorage.boxState.halfway;
                horizontalSlideState = ConfigurationStorage.horizontalSlideState.halfway;
            } else if (horizontalSlideState == ConfigurationStorage.horizontalSlideState.halfway) {
                d.leftLinkage.setPosition(0);
                d.rightLinkage.setPosition(0);
                horizontalSlideState = ConfigurationStorage.horizontalSlideState.inside;
            }
        }

        //Pressing start and back on gamepad 1 resets the position for field centric driving
        if ((start1Pressed) && (back1Pressed)) {
            d.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            d.setPoseEstimate(PoseStorage.telePowerRed);
            d.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        //Pressing left trigger more than halfway activates the left carousel wheel
        if (gamepad1.left_trigger >= 0.5) {
            d.leftServoWheel.setPower(1);
        } else {
            d.leftServoWheel.setPower(0);
        }

        //Pressing right trigger more than halfway activates the right carousel wheel
        if (gamepad1.right_trigger >= 0.5) {
            d.rightServoWheel.setPower(1);
        } else {
            d.rightServoWheel.setPower(0);
        }

        //Pressing the right trigger starts the intake and left trigger starts the outtake
        if (gamepad2.right_trigger >= 0.1 && intakeMode == ConfigurationStorage.intakeMode.manual) {
            triggerHeld = ConfigurationStorage.triggerHeld.isBeingHeld;
            d.intake.setPower(gamepad2.right_trigger * 0.75);
            range = (int) Math.floor(d.intake.getCurrentPosition() / halfWay);
        } else if (gamepad2.left_trigger >= 0.1 && intakeMode == ConfigurationStorage.intakeMode.manual) {
            triggerHeld = ConfigurationStorage.triggerHeld.isBeingHeld;
            d.intake.setPower(-gamepad2.left_trigger * 0.95);
            range = (int) Math.floor(d.intake.getCurrentPosition() / halfWay);
        } else {
            triggerHeld = ConfigurationStorage.triggerHeld.notBeingHeld;
            if (d.intake.getCurrentPosition() == Math.floor(range * halfWay) || (d.intake.getCurrentPosition() >= (Math.floor(range * halfWay) - lowerBound) && d.intake.getCurrentPosition() <= (Math.floor(range * halfWay) + upperBound))) {
                d.intake.setPower(0);
            } else {
                d.intake.setPower(-0.2);
                range = (int) Math.floor(d.intake.getCurrentPosition() / halfWay);
                d.intake.getCurrentPosition();
            }
        }

        //Once the lift goes past a certain tick threshold, the box goes to the halfway point
        if (d.rightLiftMotor.getCurrentPosition() > 200 && d.leftLiftMotor.getCurrentPosition() > 200) {
            if (gamepad2.dpad_up) {
                d.rightBox.setPosition(0.4);
                d.leftBox.setPosition(0.4);
                boxState = ConfigurationStorage.boxState.halfway;
            }

            /* If Y is pressed, the box moves and the box-state changes accordingly,
               if the motor threshold is below a certain amount and the horizontal slides are halfway,
               move the box and change the box-state */
            if (y2Pressed) {
                if (boxState == ConfigurationStorage.boxState.inside) {
                    d.rightBox.setPosition(0.4);
                    d.leftBox.setPosition(0.4);
                    boxState = ConfigurationStorage.boxState.halfway;
                } else if (boxState == ConfigurationStorage.boxState.halfway) {
                    d.rightBox.setPosition(1);
                    d.leftBox.setPosition(1);
                    boxState = ConfigurationStorage.boxState.outside;
                } else if (boxState == ConfigurationStorage.boxState.outside) {
                    d.rightBox.setPosition(0);
                    d.leftBox.setPosition(0);
                    boxState = ConfigurationStorage.boxState.inside;
                }
            }
        } else if (y2Pressed && horizontalSlideState == ConfigurationStorage.horizontalSlideState.halfway) {
            if (boxState == ConfigurationStorage.boxState.inside) {
                d.rightBox.setPosition(0.4);
                d.leftBox.setPosition(0.4);
                boxState = ConfigurationStorage.boxState.halfway;
            } else if (boxState == ConfigurationStorage.boxState.halfway) {
                d.rightBox.setPosition(1);
                d.leftBox.setPosition(1);
                boxState = ConfigurationStorage.boxState.outside;
            } else if (boxState == ConfigurationStorage.boxState.outside) {
                d.rightBox.setPosition(0);
                d.leftBox.setPosition(0);
                boxState = ConfigurationStorage.boxState.inside;
            }
        }

        //If the right stick on game-pad 2 is pressed, it restarts the lift motor encoders
        if (r3_2Pressed) {
            d.rightLiftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            d.leftLiftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

            d.rightLiftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            d.leftLiftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        //Setting the values for the ifPressed function
        start1Pressed = ifPressed(gamepad1.start);
        back1Pressed = ifPressed(gamepad1.back);
        a2Pressed = ifPressed(gamepad2.a);
        x2Pressed = ifPressed(gamepad2.x);
        y2Pressed = ifPressed(gamepad2.y);
        r3_2Pressed = ifPressed(gamepad2.right_stick_button);
        leftDpad2Pressed = ifPressed(gamepad2.dpad_left);
        booleanIncrementer = 0;
    }

    //Function used for switching powers in driving
    private void driving() {
        if (gamepad1.right_bumper) {
            power05();
        } else if (gamepad1.left_bumper) {
            power025();
        } else {
            power();
        }
    }

    //Function used to turn stick input into driving the robot
    private void power() {

        // Read pose
        // Create a vector from the gamepad x/y inputs
        // Then, rotate that vector by the inverse of that heading
        Pose2d poseEstimate = d.getPoseEstimate();

        Vector2d input = new Vector2d(
                -gamepad1.left_stick_y,
                -gamepad1.left_stick_x
        ).rotated(-poseEstimate.getHeading());

        // Pass in the rotated input + right stick value for rotation
        // Rotation is not part of the rotated input thus must be passed in separately
        d.setWeightedDrivePower(
                new Pose2d(
                        input.getX(),
                        input.getY(),
                        -gamepad1.right_stick_x
                )
        );
        // Update everything. Optometry. Etc.
        d.update();
    }

    //Function used to turn stick input into driving the robot at half speed
    private void power05() {

        // Read pose
        // Create a vector from the gamepad x/y inputs
        // Then, rotate that vector by the inverse of that heading
        Pose2d poseEstimate = d.getPoseEstimate();

        Vector2d input = new Vector2d(
                -gamepad1.left_stick_y * 0.5,
                -gamepad1.left_stick_x * 0.5
        ).rotated(-poseEstimate.getHeading());

        // Pass in the rotated input + right stick value for rotation
        // Rotation is not part of the rotated input thus must be passed in separately
        d.setWeightedDrivePower(
                new Pose2d(
                        input.getX(),
                        input.getY(),
                        -gamepad1.right_stick_x * 0.5
                )
        );
        // Update everything. Optometry. Etc.
        d.update();
    }

    //Function used to turn stick input into driving the robot at quarter speed
    private void power025() {

        // Read pose
        // Create a vector from the gamepad x/y inputs
        // Then, rotate that vector by the inverse of that heading
        Pose2d poseEstimate = d.getPoseEstimate();

        Vector2d input = new Vector2d(
                -gamepad1.left_stick_y * 0.25,
                -gamepad1.left_stick_x * 0.25
        ).rotated(-poseEstimate.getHeading());

        // Pass in the rotated input + right stick value for rotation
        // Rotation is not part of the rotated input thus must be passed in separately
        d.setWeightedDrivePower(
                new Pose2d(
                        input.getX(),
                        input.getY(),
                        -gamepad1.right_stick_x * 0.25
                )
        );
        //Update everything. Optometry. Etc.
        d.update();
    }

    //Function used to allow toggling
    private boolean ifPressed(boolean button) {
        boolean output = false;
        if (booleanArray.size() == booleanIncrementer) {
            booleanArray.add(false);
        }

        boolean buttonWas = booleanArray.get(booleanIncrementer);

        //noinspection PointlessBooleanExpression
        if (button != buttonWas && button == true) {
            output = true;
        }
        booleanArray.set(booleanIncrementer, button);

        booleanIncrementer = booleanIncrementer + 1;
        return output;
    }

    //Function used for the color sensor to run automatic outtake
    private void colorSensor() {
        if (d.colors.alpha() > 400 && intakeMode == ConfigurationStorage.intakeMode.manual && runOuttake == ConfigurationStorage.runOuttake.openToRun && triggerHeld == ConfigurationStorage.triggerHeld.isBeingHeld) {
            outtakeTime.reset();
            intakeMode = ConfigurationStorage.intakeMode.objectDetected;

            while (outtakeTime.milliseconds() < 500) {
                driving();
                action();
                d.intake.setPower(-0.95);
                range = (int) Math.floor(d.intake.getCurrentPosition() / halfWay);
            }

            intakeMode = ConfigurationStorage.intakeMode.manual;
            runOuttake = ConfigurationStorage.runOuttake.doNotRunAgain;
        }

        if (d.colors.alpha() < 400) {
            runOuttake = ConfigurationStorage.runOuttake.openToRun;
        }
    }
}